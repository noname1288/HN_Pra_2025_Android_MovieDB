package com.sun.moviedb.utils

sealed class MemberListener<out T> {
    data class OnJoin<out T>(val data: T) : MemberListener<T>()
    data class OnLeave<out T>(val data: T) : MemberListener<T>()
    data class OnChanged<out T>(val data: T) : MemberListener<T>()
    data class OnError(
        val code: Int?,
        val message: String
    ) : MemberListener<Nothing>()
}

