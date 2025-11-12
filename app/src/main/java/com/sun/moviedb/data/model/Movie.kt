package com.sun.moviedb.data.model

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class Movie(
    val id: String = "",
    val actor: List<String> = emptyList(),
    val category: List<Category> = emptyList(),
    val chieurap: Boolean = false,
    val content: String = "",
    val country: List<Country> = emptyList(),
    val director: List<String> = emptyList(),
    val episodeCurrent: String = "",
    val episodeTotal: String = "",
    val lang: String = "",
    val name: String = "",
    val originName: String = "",
    val posterUrl: String = "",
    val quality: String = "",
    val slug: String = "",
    val status: String = "",
    val thumbUrl: String = "",
    val time: String = "",
    val trailerUrl: String = "",
    val type: String = "",
    val view: Int = 0,
    val year: Int = 0
) : Parcelable
