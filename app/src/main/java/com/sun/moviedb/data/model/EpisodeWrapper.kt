package com.sun.moviedb.data.model

data class EpisodeWrapper(
    val serverData: List<EpisodeModel> = emptyList(),
    val serverName: String = ""
)
