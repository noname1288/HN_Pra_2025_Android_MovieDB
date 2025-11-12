package com.sun.moviedb.screen.watch_new

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.session.MediaController
import com.sun.moviedb.data.model.Movie
import com.sun.moviedb.data.repository.source.local.PrefHelper
import com.sun.moviedb.databinding.ActivityNewWatchMovieBinding

class NewWatchMovieActivity : AppCompatActivity(), NewWatchMovieContract.View {
    private lateinit var binding: ActivityNewWatchMovieBinding
    private lateinit var presenter: NewWatchMovieContract.Presenter
    private var mediaController: MediaController? = null

    private var m3u8Link: String? = null
    private var movieInfo: Movie? = null

    private val tag = "NewWatchMovieActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewWatchMovieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUi()

        initData()

        if (!m3u8Link.isNullOrEmpty() && movieInfo != null)
            presenter.initializeMediaController(m3u8Link!!, movieInfo!!)
        else
            showError("Video link is invalid")

        Log.d(tag, "onCreate() called")

    }

    override fun onStart() {
        super.onStart()
        Log.d(tag, "onStart() called")
    }

    override fun onRestart() {
        super.onRestart()
        presenter.restorePlayerStateAndPlay()
        Log.d(tag, "onRestart() called")
    }

    override fun onResume() {
        super.onResume()
        Log.d(tag, "onResume() called")
    }

    override fun onPause() {
        super.onPause()
        Log.d(tag, "onPause() called")
    }

    override fun onStop() {
        super.onStop()
        presenter.savePlayerStateAndPause()
        Log.d(tag, "onStop() called")
    }

    override fun onDestroy() {
        presenter.releaseMediaController()
        presenter.detachView()
        Log.d(tag, "onDestroy() called")
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(tag, "onSaveInstanceState() called")
        outState.putLong("pos", mediaController?.currentPosition ?: 0L)
        outState.putBoolean("pwr", mediaController?.isPlaying == true)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(tag, "onRestoreInstanceState() called")
        val pos = savedInstanceState.getLong("pos", 0L)
        val pwr = savedInstanceState.getBoolean("pwr", false)
        mediaController?.seekTo(pos)
        if (pwr) mediaController?.play() else mediaController?.pause()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(tag, "onConfigurationChanged() called: ${newConfig.orientation}")
    }

    private fun initData() {
        m3u8Link = intent.getStringExtra(M3U8_LINK)
        movieInfo = intent.getParcelableExtra<Movie>(OBJECT_MOVIE)

        presenter = NewWatchMoviePresenter(this, PrefHelper(this))
        presenter.attachView(this)
    }

    override fun showLoading(isLoading: Boolean) {}

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun hideLoading() {}

    override fun showPlayerView(mediaController: MediaController) {
        binding.playerViewNew.player = mediaController
        this.mediaController = mediaController
    }

    override fun getCurrentPositionMs(): Long =
        this.mediaController?.currentPosition ?: 0L


    override fun isPlayingNow(): Boolean =
        this.mediaController?.isPlaying == true

    override fun seekTo(positionMs: Long) {
        this.mediaController?.seekTo(positionMs)
    }

    override fun setPlayWhenReady(ready: Boolean) {
        if (ready) this.mediaController?.play() else this.mediaController?.pause()
    }

    override fun mediaId(): String {
        return m3u8Link ?: ""
    }

    companion object {
        const val M3U8_LINK = "m3u8_link"
        const val OBJECT_MOVIE = "object_movie"
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.playerViewNew).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
