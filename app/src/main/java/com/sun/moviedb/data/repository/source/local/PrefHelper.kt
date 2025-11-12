package com.sun.moviedb.data.repository.source.local

import android.content.Context
import android.content.SharedPreferences

class PrefHelper(context: Context) {
    private val PREF_NAME = "movie_db_pref"

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveState(mediaId: String, state: PlaybackStateModel) {
        sharedPref.edit()
            .putLong("${mediaId}_position", state.positionMs)
            .putBoolean("${mediaId}_playWhenReady", state.playWhenReady)
            .apply()
    }

    fun loadFromLocal(mediaId: String): PlaybackStateModel? {
        if (!sharedPref.contains("${mediaId}_position")) {
            return null
        }

        val positionMs = sharedPref.getLong("${mediaId}_position", 0L)
        val playWhenReady = sharedPref.getBoolean("${mediaId}_playWhenReady", true)

        return PlaybackStateModel(positionMs, playWhenReady)
    }

    fun clearState(mediaId: String) {
        sharedPref.edit()
            .remove("${mediaId}_position")
            .remove("${mediaId}_playWhenReady")
            .apply()
    }
}
