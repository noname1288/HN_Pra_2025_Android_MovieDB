package com.sun.moviedb.data.repository.source.remote.parse

import com.sun.moviedb.data.model.Movie
import com.sun.moviedb.data.repository.source.remote.dto.MovieDetailResponse
import org.json.JSONObject

fun String.toDetailMovieResponse(): MovieDetailResponse {
    val root = JSONObject(this)
    val status = root.optBoolean("status", false)
    val msg = root.optString("msg")
    val movie = root.optJSONObject("movie")?.toMovie() ?: Movie()
    val episodes = root.optJSONArray("episodes")?.toEpisodeList() ?: emptyList()
    return MovieDetailResponse(
        episodeWrappers = episodes,
        movie = movie,
        msg = msg,
        status = status
    )
}
