package com.sun.moviedb.data.repository.source.remote.dto

import com.sun.moviedb.data.model.EpisodeWrapper
import com.sun.moviedb.data.model.Movie

data class MovieDetailResponse(
    val episodeWrappers: List<EpisodeWrapper>,
    val movie: Movie,
    val msg: String,
    val status: Boolean
)
