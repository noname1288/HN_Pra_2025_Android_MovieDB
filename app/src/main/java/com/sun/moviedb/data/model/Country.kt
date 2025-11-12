package com.sun.moviedb.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Country(
    val id: String = "",
    val name: String = "",
    val slug: String = ""
): Parcelable
