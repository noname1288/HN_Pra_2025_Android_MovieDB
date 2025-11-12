package com.sun.moviedb.screen.watchMovie

import android.os.Bundle
import androidx.media3.common.Player
import com.sun.moviedb.data.model.Member
import com.sun.moviedb.utils.base.BasePresenter
import com.sun.moviedb.utils.base.BaseView

interface WatchMovieContract {
    interface View : BaseView {
        fun initializePlayerView(player: Player)
        fun releasePlayerView()
        fun showPlayerError(message: String)
        fun enterFullscreenMode()
        fun exitFullscreenMode()
        fun getInitialPlaybackPosition(): Long
        fun getInitialPlayWhenReady(): Boolean
        fun setOriginalOrientation(orientation: Int)
        fun getOriginalOrientation(): Int
        fun popView()
        fun showAddedMember(memberName: String)
        fun showLeftMember(memberName: String)
        fun showRemoveCurrentUserMessage(message: String)
        fun updateMemberList(members: List<Member>)

        fun executeRemotePlay()
        fun executeRemotePause()
        fun executeRemoteSeek(positionMs: Long)
        fun showSyncError(message: String)
        fun showSyncSuccess(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun onActivityCreated(m3u8Link: String?,
                              initialPlaybackPosition: Long,
                              initialPlayWhenReady: Boolean
        )

        fun onSaveInstanceStateRequested(): Bundle

        fun updateRoomId(roomId: String)
        fun observeMembers(roomId: String)
        fun removeChosenMember(roomId: String, memberId: String)
        fun onSearchUserClicked()
        fun onInviteUserToRoom(userId: String)
        fun getCachedMembers() : List<Member>
        fun checkHost(roomId: String, memberId: String): Boolean
        fun changeHost(roomId: String, newMemberId: String)

        fun initializeSyncController(roomId: String?)
        fun onLocalPlayerPlayAction()
        fun onLocalPlayerPauseAction()
        fun onLocalPlayerSeekAction(positionMs: Long)
        fun stopSyncController()

        fun removeListener(roomId: String)
    }
}