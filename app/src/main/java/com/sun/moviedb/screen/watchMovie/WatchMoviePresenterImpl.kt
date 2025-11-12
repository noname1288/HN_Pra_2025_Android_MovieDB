package com.sun.moviedb.screen.watchMovie

import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.sun.moviedb.data.model.Member
import com.sun.moviedb.data.repository.ControllerRepository
import com.sun.moviedb.data.repository.impl.ControllerRepositoryImpl
import com.sun.moviedb.data.repository.rtdb.member.MemberRepository
import com.sun.moviedb.data.repository.source.remote.NetworkResult
import com.sun.moviedb.utils.CommandStringParser
import com.sun.moviedb.utils.CommandType
import com.sun.moviedb.utils.MemberListener
import com.sun.moviedb.utils.session.RoomSession
import com.sun.moviedb.utils.session.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class WatchMoviePresenterImpl(
    private val contextProvider: () -> WatchMovieActivity?, // For context if absolutely needed
    private val memberRepository: MemberRepository,
    // Inject ControllerRepository and FirebaseAuth for testability
    private val controllerRepository: ControllerRepository = ControllerRepositoryImpl(
        FirebaseDatabase.getInstance()
    ),
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : WatchMovieContract.Presenter {

    private var view: WatchMovieContract.View? = null
    private var player: ExoPlayer? = null
    private var m3u8Link: String? = null
    private var currentRoomId: String? = null
    private val TAG = "WatchMoviePresenter"

    // --- Controller Sync Logic Properties ---
    private var currentUserId: String? = null
    private var commandListenerJob: Job? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isProcessingRemoteCommand = false

    /*
    * Control members
    * */
    private var _member = mutableListOf<Member>()


    override fun attachView(view: WatchMovieContract.View) {
        this.view = view
        currentUserId = firebaseAuth.currentUser?.uid
    }

    override fun detachView() {
        player?.release()
        player = null
        stopSyncController() // Ensure controller listener is stopped
        presenterScope.cancel() // Cancel presenter's coroutine scope
        this.view = null
    }

    override fun onActivityCreated(
        m3u8Link: String?,
        initialPlaybackPosition: Long,
        initialPlayWhenReady: Boolean
    ) {
        this.m3u8Link = m3u8Link
        if (m3u8Link.isNullOrEmpty()) {
            view?.showPlayerError("No video link provided.")
            return
        }

        contextProvider()?.let { context ->
            player = ExoPlayer.Builder(context).build().also { exoPlayer ->
                val mediaItem = MediaItem.fromUri(m3u8Link)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.seekTo(initialPlaybackPosition)
                exoPlayer.playWhenReady = initialPlayWhenReady
                exoPlayer.prepare()
                view?.initializePlayerView(exoPlayer)
            }
        }
    }

    override fun onSaveInstanceStateRequested(): Bundle {

        return Bundle()
    }

    override fun updateRoomId(roomId: String) {
        RoomSession.updateRoomId(roomId)
    }

    override fun observeMembers(roomId: String) {
        memberRepository.listenMemberChanged(roomId) { result ->
            when (result) {
                is MemberListener.OnJoin<Member> -> {
                    val memberData = result.data
                    _member.add(memberData)
                    view?.showAddedMember(memberData.memberName)
                    view?.updateMemberList(_member)
                }

                is MemberListener.OnLeave<Member> -> {
                    val memberData = result.data
                    _member.remove(memberData)

                    if (UserSession.userId == memberData.memberId) {
                        view?.popView()
                        view?.showRemoveCurrentUserMessage("Bạn đã bị đá khỏi phòng :<")
                    } else {
                        view?.showLeftMember(memberData.memberName)
                    }
                    view?.updateMemberList(_member)
                }

                is MemberListener.OnChanged<Member> -> {
                    val updatedMember = result.data
                    val index = _member.indexOfFirst { it.memberId == updatedMember.memberId }
                    if (index > 0){
                        _member[index] = updatedMember
                        view?.updateMemberList(_member)
                    }
                }

                is MemberListener.OnError -> {
                    view?.showError(result.message)
                }
            }
        }
    }

    override fun removeChosenMember(roomId: String, memberId: String) {
        memberRepository.removeMember(roomId, memberId) { result ->
            when (result) {
                is NetworkResult.OnError -> {
                    view?.showError(result.message)
                }

                is NetworkResult.OnSuccess -> {/* do nothing*/
                }
            }
        }
    }

    override fun onSearchUserClicked() {

    }

    override fun onInviteUserToRoom(userId: String) {

    }

    override fun getCachedMembers(): List<Member> {
        return _member.toList()
    }

    override fun checkHost(roomId: String, memberId: String): Boolean {
        var checkedHost = false
        memberRepository.getMemberById(roomId, memberId){ result ->
            when(result){
                is NetworkResult.OnError -> {
                    view?.showError(result.message)
                }
                is NetworkResult.OnSuccess<Member?> -> {
                    val user = result.data
                    user?.let {
                        if (it.host){
                            checkedHost = true
                        }
                    }
                }
            }
        }

        return checkedHost
    }

    override fun changeHost(roomId: String, newMemberId: String) {
        memberRepository.changeHost(roomId, newMemberId){ result ->
            when(result){
                is NetworkResult.OnError -> {
                    view?.showError(result.message)
                }
                is NetworkResult.OnSuccess<Unit> -> {}
            }

        }
    }

    override fun initializeSyncController(roomId: String?) {
        this.currentRoomId = roomId
        if (currentRoomId.isNullOrBlank()) {
            Log.e(TAG, "Room ID is null or blank. Cannot initialize sync controller.")
            view?.showSyncError("Invalid Room ID for sync.")
            return
        }
        if (currentUserId.isNullOrBlank()) {
            Log.w(TAG, "User not logged in. Sync features may be limited.")
        }
        startListeningForCommands()
    }

    private fun startListeningForCommands() {
        val roomId = currentRoomId ?: return
        commandListenerJob?.cancel()
        commandListenerJob = presenterScope.launch {
            controllerRepository.listenForRoomCommandString(roomId)
                .mapNotNull { CommandStringParser.parseCommandString(it) }
                .catch { exception ->
                    Log.e(TAG, "Error collecting/parsing room commands for $roomId", exception)
                    view?.showSyncError("Error receiving sync commands.")
                }
                .collectLatest { parsedCommand ->
                    Log.i(TAG, "Presenter received command: $parsedCommand")
                    processReceivedCommand(parsedCommand)
                }
        }
        Log.d(TAG, "Presenter started listening for commands in room: $roomId")
    }

    override fun stopSyncController() {
        commandListenerJob?.cancel()
        commandListenerJob = null
        Log.d(TAG, "Presenter stopped listening for commands.")
    }

    override fun removeListener(roomId: String) {
        memberRepository.removeChildEventListener(roomId)
    }

    private fun processReceivedCommand(parsedCommand: CommandStringParser.ParsedCommand) {
        if (parsedCommand.senderId == currentUserId && isProcessingRemoteCommand) {
            // Avoid loop if we just sent this and player event triggered another send.
            // More robust check might be needed.
            Log.d(TAG, "Skipping own command that might be a loop: ${parsedCommand.type}")
            return
        }

        isProcessingRemoteCommand = true
        presenterScope.launch {
            try {
                when (parsedCommand.type) {
                    CommandType.PLAY -> view?.executeRemotePlay()
                    CommandType.PAUSE -> view?.executeRemotePause()
                    CommandType.SEEK -> {
                        parsedCommand.value?.toLongOrNull()?.let { timeMillis ->
                            view?.executeRemoteSeek(timeMillis)
                        }
                    }

                    else -> Log.w(
                        TAG,
                        "Presenter received unknown command type: ${parsedCommand.type}"
                    )
                }
            } finally {
                kotlinx.coroutines.delay(100)
                isProcessingRemoteCommand = false
            }
        }
    }

    private fun sendCommandToFirebase(commandType: String, value: String? = null) {
        val roomId = currentRoomId
        val sender = currentUserId
        if (roomId.isNullOrBlank() || sender.isNullOrBlank()) {
            Log.w(TAG, "Cannot send command. Room/Sender ID blank. Room: $roomId, Sender: $sender")
            return
        }

        val commandString = CommandStringParser.createCommandString(
            type = commandType,
            senderId = sender,
            value = value
        )
        presenterScope.launch {
            val result = controllerRepository.sendRoomCommandString(roomId, commandString)
            result.onSuccess {
                Log.i(TAG, "Presenter sent command '$commandString' to room $roomId.")
            }.onFailure { exception ->
                Log.e(TAG, "Presenter failed to send command '$commandString'", exception)
                view?.showSyncError("Failed to send sync command.")
            }
        }
    }

    override fun onLocalPlayerPlayAction() {
        if (isProcessingRemoteCommand) return
        Log.d(TAG, "Presenter: Local PLAY action received")
        sendCommandToFirebase(CommandType.PLAY)
    }

    override fun onLocalPlayerPauseAction() {
        if (isProcessingRemoteCommand) return
        Log.d(TAG, "Presenter: Local PAUSE action received")
        sendCommandToFirebase(CommandType.PAUSE)
    }

    override fun onLocalPlayerSeekAction(positionMs: Long) {
        if (isProcessingRemoteCommand) return
        Log.d(TAG, "Presenter: Local SEEK action to $positionMs received")
        sendCommandToFirebase(CommandType.SEEK, positionMs.toString())
    }
}
