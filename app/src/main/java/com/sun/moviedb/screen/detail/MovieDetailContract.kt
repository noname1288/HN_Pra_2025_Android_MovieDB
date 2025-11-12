package com.sun.moviedb.screen.detail

import com.sun.moviedb.data.model.EpisodeWrapper
import com.sun.moviedb.data.model.Movie
import com.sun.moviedb.utils.base.BasePresenter
import com.sun.moviedb.utils.base.BaseView

interface MovieDetailContract {
    interface View : BaseView {
        fun onGetDetailSuccess(movie: Movie, episodeWrappers: List<EpisodeWrapper>)
        fun onCheckFavorite(isFavorite: Boolean)
        fun onMovieToFirebaseSuccess(message: String)
        fun onAddSuccess(message: String)
        fun showLoading2(isLoading: Boolean)
    }

    interface Presenter : BasePresenter<View> {
        fun getDetail(slug: String)
        fun onFavClicked(movie: Movie, isFavourite: Boolean, userId: String)
        fun checkFavorite(movieId: String, userId: String)
        fun createRoom(movie: Movie)
        fun addCurrentMember(roomId: String)
        fun deleteCurrentMember(roomId: String)
        fun removeMemberListener(roomId: String)
    }
}
