package com.sun.moviedb.screen.watch_new

import androidx.media3.session.MediaController
import com.sun.moviedb.data.model.Movie
import com.sun.moviedb.utils.base.BasePresenter
import com.sun.moviedb.utils.base.BaseView

interface NewWatchMovieContract {
    interface View : BaseView{
        fun hideLoading()
        fun showPlayerView(mediaController: MediaController)
        fun getCurrentPositionMs(): Long
        fun isPlayingNow(): Boolean
        fun seekTo(positionMs: Long)
        fun setPlayWhenReady(ready: Boolean)
        fun mediaId(): String
    }

    interface Presenter: BasePresenter<View>{
        fun initializeMediaController(videoUrl: String, movieInfo : Movie)
        fun releaseMediaController()

        fun restorePlayerStateAndPlay()
        fun savePlayerStateAndPause()
    }
}
