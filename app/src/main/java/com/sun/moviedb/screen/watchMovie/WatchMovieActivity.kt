package com.sun.moviedb.screen.watchMovie

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.sun.moviedb.data.model.Member
import com.sun.moviedb.databinding.ActivityWatchMovieBinding
import com.sun.moviedb.screen.chat.ChatFragment
import com.sun.moviedb.screen.room.RoomFragment
import com.sun.moviedb.screen.searchUser.SearchUserFragment
import com.sun.moviedb.utils.AppLocator
import com.sun.moviedb.utils.base.BaseActivity
import com.sun.moviedb.utils.session.RoomSession
import kotlinx.coroutines.launch

class WatchMovieActivity : BaseActivity<ActivityWatchMovieBinding>(), WatchMovieContract.View {

    private lateinit var presenter: WatchMovieContract.Presenter

    private var activityOriginalOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    private var roomId: String? = null
    private var m3u8Link: String? = null
    private var initialPlaybackPosition = 0L
    private var initialPlayWhenReady = true
    private var lastReportedIsPlayingToPresenter = false
    private var lastReportedSeekPositionToPresenter = -1L
    private var isPlayerInternallyChanging = false

    private var currentFragmentTag: String? = null
    private var isFragmentVisible = false

    private val TAG = "WatchMovieActivity"

    override fun getViewBinding(): ActivityWatchMovieBinding {
        return ActivityWatchMovieBinding.inflate(layoutInflater)
    }

    override fun initData() {
        super.initData()
        presenter = WatchMoviePresenterImpl({ this }, AppLocator.memberRepository)
        presenter.attachView(this)

        roomId = intent.getStringExtra(ARG_ROOM_ID)
        m3u8Link = intent.getStringExtra(ARG_M3U8_LINK)
        RoomSession.updateMovieLink(m3u8Link ?: "")

        if (intent.extras != null && intent.extras!!.containsKey(SAVED_PLAYBACK_POSITION)) {
            initialPlaybackPosition = intent.extras!!.getLong(SAVED_PLAYBACK_POSITION, 0L)
            initialPlayWhenReady = intent.extras!!.getBoolean(SAVED_PLAY_WHEN_READY, true)
        }
        activityOriginalOrientation = requestedOrientation

        presenter.onActivityCreated(m3u8Link, initialPlaybackPosition, initialPlayWhenReady)

        onGroupButtonClicked()
        onChatButtonClicked()
        synchronizeButtonWithPlayerState()
        showOrHideFragment()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false) // Key line for going edge-to-edge
        WindowInsetsControllerCompat(window, binding.playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars()) // Hides status and navigation bars
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        supportActionBar?.hide()
    }

    override fun onStart() {
        super.onStart()
        roomId?.let { currentRoomId ->
            if (currentRoomId.isNotBlank()) {
                presenter.updateRoomId(currentRoomId)
                presenter.observeMembers(currentRoomId)
                presenter.initializeSyncController(currentRoomId)
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        initialPlaybackPosition = savedInstanceState.getLong(SAVED_PLAYBACK_POSITION, 0L)
        initialPlayWhenReady = savedInstanceState.getBoolean(SAVED_PLAY_WHEN_READY, true)
    }

    override fun onResume() {
        super.onResume()
        binding.playerView.player?.playWhenReady = lastReportedIsPlayingToPresenter
    }

    override fun onPause() {
        super.onPause()
        binding.playerView.player?.let {
            lastReportedIsPlayingToPresenter = it.playWhenReady
        }
//        val currentPosition = binding.playerView.player?.currentPosition ?: initialPlaybackPosition
//        val currentPlayWhenReady = binding.playerView.player?.playWhenReady ?: initialPlayWhenReady
    }

    override fun onStop() {
        super.onStop()
        presenter.stopSyncController()
        roomId?.let { presenter.removeListener(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val stateBundle = presenter.onSaveInstanceStateRequested()
        outState.putLong(
            SAVED_PLAYBACK_POSITION,
            stateBundle.getLong(SAVED_PLAYBACK_POSITION, initialPlaybackPosition)
        )
        outState.putBoolean(
            SAVED_PLAY_WHEN_READY,
            stateBundle.getBoolean(SAVED_PLAY_WHEN_READY, initialPlayWhenReady)
        )
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()

        releasePlayerView()

        presenter.detachView()
    }

    override fun showLoading(isLoading: Boolean) {

    }

    override fun showError(message: String) {

    }

    private val localPlayerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (isPlayerInternallyChanging) return

            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
                if (playWhenReady && !lastReportedIsPlayingToPresenter) {
                    Log.d(TAG, "View: Local PLAY action")
                    presenter.onLocalPlayerPlayAction()
                    lastReportedIsPlayingToPresenter = true
                } else if (!playWhenReady && lastReportedIsPlayingToPresenter) {
                    Log.d(TAG, "View: Local PAUSE action")
                    presenter.onLocalPlayerPauseAction()
                    lastReportedIsPlayingToPresenter = false
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlayerInternallyChanging) return
            binding.playerView.player?.let {
                if (it.playbackState == Player.STATE_READY) {
                    if (isPlaying != lastReportedIsPlayingToPresenter) {
                        lastReportedIsPlayingToPresenter = isPlaying
                    }
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (isPlayerInternallyChanging) return

            if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                val currentPos = newPosition.positionMs
                if (kotlin.math.abs(currentPos - lastReportedSeekPositionToPresenter) > 1000) {
                    Log.d(TAG, "View: Local SEEK action to $currentPos")
                    presenter.onLocalPlayerSeekAction(currentPos)
                    lastReportedSeekPositionToPresenter = currentPos
                }
            }
        }
    }

    override fun initializePlayerView(player: Player) {
        binding.playerView.player = player
        binding.playerView.player?.removeListener(localPlayerListener)
        binding.playerView.player?.addListener(localPlayerListener)
        player.playWhenReady = initialPlayWhenReady
        player.seekTo(initialPlaybackPosition)
        lastReportedIsPlayingToPresenter = player.isPlaying
        lastReportedSeekPositionToPresenter = player.currentPosition
    }

    override fun releasePlayerView() {
        binding.playerView.player?.removeListener(localPlayerListener)
        binding.playerView.player?.release()
        binding.playerView.player = null
    }

    override fun executeRemotePlay() {
        Log.d(TAG, "View: Executing Remote PLAY")
        isPlayerInternallyChanging = true
        binding.playerView.player?.play()
        lastReportedIsPlayingToPresenter = true
        lifecycleScope.launch { kotlinx.coroutines.delay(100); isPlayerInternallyChanging = false }
    }

    override fun executeRemotePause() {
        Log.d(TAG, "View: Executing Remote PAUSE")
        isPlayerInternallyChanging = true
        binding.playerView.player?.pause()
        lastReportedIsPlayingToPresenter = false
        lifecycleScope.launch { kotlinx.coroutines.delay(100); isPlayerInternallyChanging = false }
    }

    override fun executeRemoteSeek(positionMs: Long) {
        Log.d(TAG, "View: Executing Remote SEEK to $positionMs")
        val player = binding.playerView.player ?: return
        if (kotlin.math.abs(player.currentPosition - positionMs) > 1500) {
            isPlayerInternallyChanging = true
            player.seekTo(positionMs)
            lastReportedSeekPositionToPresenter = positionMs
            lifecycleScope.launch { kotlinx.coroutines.delay(100); isPlayerInternallyChanging = false }
        }
    }

    override fun showSyncError(message: String) {
        Toast.makeText(this, "Sync Error: $message", Toast.LENGTH_SHORT).show()
    }

    override fun showSyncSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showPlayerError(message: String) {
        Toast.makeText(this, "Player Error: $message", Toast.LENGTH_LONG).show()
    }

    override fun enterFullscreenMode() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        supportActionBar?.hide()
    }

    override fun exitFullscreenMode() {
        requestedOrientation = activityOriginalOrientation
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, binding.playerView)
            .show(WindowInsetsCompat.Type.systemBars())
        supportActionBar?.show()
    }

    override fun getInitialPlaybackPosition(): Long = initialPlaybackPosition

    override fun getInitialPlayWhenReady(): Boolean = initialPlayWhenReady

    override fun setOriginalOrientation(orientation: Int) {
        this.activityOriginalOrientation = orientation
    }

    override fun getOriginalOrientation(): Int = this.activityOriginalOrientation

    override fun popView() {
        finish()
    }

    override fun showAddedMember(memberName: String) {
        Log.d(TAG, "showAddedMember: $memberName")
        Toast.makeText(this, "$memberName has joined the room", Toast.LENGTH_SHORT).show()
    }

    override fun showLeftMember(memberName: String) {
        Log.d(TAG, "showLeftMember: $memberName")
        Toast.makeText(this, "$memberName has left the room", Toast.LENGTH_SHORT).show()
    }

    override fun showRemoveCurrentUserMessage(message: String) {
        Log.d(TAG, "showRemoveCurrentUser: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun updateMemberList(members: List<Member>) {
        val roomFragment = supportFragmentManager.findFragmentByTag(ROOM_FRAGMENT_TAG)
        if (roomFragment is RoomFragment) {
            roomFragment.updateMembers(members)
        }
    }

    private fun showOrHideFragment() {
        binding.containerOverlay.setOnClickListener {
            if (isFragmentVisible) {
                hideFragment()
            }
        }
    }

    private fun synchronizeButtonWithPlayerState() {
        binding.playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
            if (visibility == View.VISIBLE && roomId != null) {
                binding.btnChat.animate().alpha(1f).setDuration(200).start()
                binding.btnRoom.animate().alpha(1f).setDuration(200).start()
                binding.btnRoom.visibility = View.VISIBLE
                binding.btnChat.visibility = View.VISIBLE
            } else {
                binding.btnChat.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.btnChat.visibility = View.GONE
                }.start()
                binding.btnRoom.animate().alpha(0f).setDuration(200).withEndAction {
                    binding.btnRoom.visibility = View.GONE
                }.start()
            }
        })
    }

    private fun onChatButtonClicked() {
        binding.btnChat.setOnClickListener {
            roomId?.let {
                val chatFragment = ChatFragment()
                toggleFragment(chatFragment, CHAT_FRAGMENT_TAG)
            }
        }
    }

    private fun onGroupButtonClicked() {
        binding.btnRoom.setOnClickListener {
            roomId?.let {
                val fragment = RoomFragment()
                fragment.setWatchPresenter(presenter)
                toggleFragment(fragment, ROOM_FRAGMENT_TAG)
            }
            supportFragmentManager.beginTransaction()
                .replace(binding.containerSearchUser.id, SearchUserFragment())
                .commit()
        }
    }

    private fun toggleFragment(fragment: Fragment, tagFragment: String) {
        val container1 = binding.containterRoomChat
        val container2 = binding.containerSearchUser
        val containerOverlay = binding.containerOverlay

        if (currentFragmentTag == tagFragment) {
            supportFragmentManager.findFragmentByTag(tagFragment)?.let {
                supportFragmentManager.beginTransaction().remove(it).commit()
            }
            containerOverlay.visibility = View.GONE
            containerOverlay.isClickable = false
            container1.visibility = View.GONE
            container2.visibility = View.GONE
            currentFragmentTag = null
            isFragmentVisible = false
        } else {
            supportFragmentManager.beginTransaction()
                .replace(container1.id, fragment, tagFragment)
                .commit()
            containerOverlay.visibility = View.VISIBLE
            containerOverlay.isClickable = true
            container1.visibility = View.VISIBLE
            if (tagFragment == CHAT_FRAGMENT_TAG)
                container2.visibility = View.GONE
            else container2.visibility = View.VISIBLE
            currentFragmentTag = tagFragment
            isFragmentVisible = true
        }
    }

    private fun hideFragment() {
        val container1 = binding.containterRoomChat
        val container2 = binding.containerSearchUser
        val containerOverlay = binding.containerOverlay

        currentFragmentTag?.let {
            supportFragmentManager.findFragmentByTag(it)?.let { fragment ->
                supportFragmentManager.beginTransaction().remove(fragment).commit()
            }
        }

        containerOverlay.visibility = View.GONE
        containerOverlay.isClickable = false
        container1.visibility = View.GONE
        container2.visibility = View.GONE

        currentFragmentTag = null
        isFragmentVisible = false
    }

    companion object {
        const val ARG_M3U8_LINK = "m3u8_link"
        const val ARG_ROOM_ID = "room_id"
        private const val SAVED_PLAYBACK_POSITION = "playbackPosition"
        private const val SAVED_PLAY_WHEN_READY = "playWhenReady"
        private const val CHAT_FRAGMENT_TAG = "CHAT_FRAGMENT"
        private const val ROOM_FRAGMENT_TAG = "ROOM_FRAGMENT"
    }
}
