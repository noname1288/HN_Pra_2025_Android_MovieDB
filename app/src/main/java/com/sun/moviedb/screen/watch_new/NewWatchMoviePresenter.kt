package com.sun.moviedb.screen.watch_new

import android.content.ComponentName
import android.content.Context
import android.media.session.PlaybackState
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.sun.moviedb.data.model.Movie
import com.sun.moviedb.data.repository.source.local.PlaybackStateModel
import com.sun.moviedb.data.repository.source.local.PrefHelper
import com.sun.moviedb.service.MyMediaService

class NewWatchMoviePresenter(
    private val context: Context,
    private val store: PrefHelper
) : NewWatchMovieContract.Presenter {
    private var _view: NewWatchMovieContract.View? = null
    private lateinit var mediaControllerFuture: ListenableFuture<MediaController>
    private val mediaController: MediaController?
        get() = if (mediaControllerFuture.isDone) mediaControllerFuture.get() else null
    private var cached: PlaybackStateModel? = null


    override fun attachView(view: NewWatchMovieContract.View) {
        _view = view
        cached =store.loadFromLocal(view.mediaId())
    }

    override fun detachView() {
        _view = null
    }

    override fun initializeMediaController(videoUrl: String, movieInfo: Movie) {
        val sessionToken = SessionToken(context, ComponentName(context, MyMediaService::class.java))

        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        mediaControllerFuture.addListener(
            {
                mediaController?.let {
                    _view?.showPlayerView(it)
                    loadMedia(videoUrl, movieInfo)
                }
            }, MoreExecutors.directExecutor()
        )
    }

    override fun releaseMediaController() {
        MediaController.releaseFuture(mediaControllerFuture)
    }

    override fun restorePlayerStateAndPlay() {
        val v = _view ?: return
        val state = store.loadFromLocal(v.mediaId())
        if (state != null) {
            v.seekTo(state.positionMs)
            v.setPlayWhenReady(true)
            cached = state
        }
    }

    override fun savePlayerStateAndPause() {
        val v = _view ?: return
        val state = PlaybackStateModel(
            positionMs = v.getCurrentPositionMs(),
            playWhenReady = v.isPlayingNow()
        )
        store.saveState(v.mediaId(), state)
        v.setPlayWhenReady(false)
        cached = state
    }

    private fun loadMedia(videoUrl: String, movie: Movie) {
        val mediaItem = MediaItem.Builder().setUri(videoUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(movie.name)
                    .setGenre(movie.category.get(0).name)
                    .setArtist(movie.director.get(0))
                    .setArtworkUri(Uri.parse(movie.thumbUrl))
                    .build()
            )
            .build()

        mediaController?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
    }
}
