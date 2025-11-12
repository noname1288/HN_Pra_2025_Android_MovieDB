package com.sun.moviedb.screen.detail

import com.sun.moviedb.data.model.Member
import com.sun.moviedb.data.model.Movie
import com.sun.moviedb.data.model.Room
import com.sun.moviedb.data.repository.rtdb.member.MemberRepository
import com.sun.moviedb.data.repository.rtdb.room.RoomRepository
import com.sun.moviedb.data.repository.source.MovieRepository
import com.sun.moviedb.data.repository.source.firebase.entity.MovieFirebaseEntity
import com.sun.moviedb.data.repository.source.remote.NetworkResult
import com.sun.moviedb.data.repository.source.remote.dto.MovieDetailResponse
import com.sun.moviedb.utils.session.UserSession

class MovieDetailPresenter
internal constructor(
    private val mMovieRepository: MovieRepository?,
    private val roomRepository: RoomRepository,
    private val memberRepository: MemberRepository
) : MovieDetailContract.Presenter {
    private var mView: MovieDetailContract.View? = null


    override fun attachView(view: MovieDetailContract.View) {
        this.mView = view
    }

    override fun detachView() {
        mView = null
    }

    override fun getDetail(slug: String) {
        mMovieRepository?.getDetailMovie(slug) { result ->
            when (result) {
                is NetworkResult.OnSuccess<MovieDetailResponse> -> {
                    val movie = result.data.movie
                    val episodes = result.data.episodeWrappers

                    mView?.onGetDetailSuccess(movie, episodes)
                }

                is NetworkResult.OnError -> {
                    val errorMessage = result.message
                    mView?.showError(errorMessage)
                }
            }
        }
    }

    override fun checkFavorite(movieId: String, userId: String) {
        mMovieRepository?.getFavoriteById(movieId) { movie ->
            mView?.onCheckFavorite(movie != null)
        }
    }

    override fun onFavClicked(movie: Movie, isFavourite: Boolean, userId: String) {
        if (isFavourite) {
            mMovieRepository?.deleteFavorite(movie.id)
            checkFavorite(movie.id, userId)
            mMovieRepository?.removeFavoriteMovieFromFirebase(userId, movie.id) { isSuccess ->
                mView?.onMovieToFirebaseSuccess("Removed from Firebase ${if (isSuccess) "successfully" else "failed"}")
            }

        } else {
            mMovieRepository?.insertFavorite(movie)
            checkFavorite(movie.id, userId)
            mMovieRepository?.addFavoriteMovieToFirebase(
                userId,
                MovieFirebaseEntity.fromMovie(movie)
            ) { isSuccess ->
                mView?.onMovieToFirebaseSuccess("Added to Firebase ${if (isSuccess) "successfully" else "failed"}")
            }
        }
    }

    override fun createRoom(movie: Movie) {
        val userID = UserSession.userId.run {
            if (this.isNullOrEmpty()) {
                throw Exception("Current user not found")
            } else this
        }
        val room = Room(
            roomName = movie.name,
            roomCode = movie.slug,
            createAt = System.currentTimeMillis(),
            createBy = userID
        )

        mView?.showLoading2(true)
        roomRepository.addRoom(room) { result ->
            when (result) {
                is NetworkResult.OnSuccess -> {
                    // Room created successfully
                    mView?.onAddSuccess("Room created successfully")
                }

                is NetworkResult.OnError -> {
                    // Failed to create room
                    mView?.showError(result.message)
                }
            }
            mView?.showLoading2(false)
        }
    }

    override fun addCurrentMember(roomId: String) {
        mView?.showLoading2(true)

        val userID = UserSession.userId.run {
            if (this.isNullOrEmpty()) {
                throw Exception("Current user not found")
            } else this
        }

        val member = Member(
            memberId = userID,
            memberName = UserSession.userName ?: "Unknown",
            linkAvatar = UserSession.linkAvatar ?: "",
            joinAt = System.currentTimeMillis(),
            host = true
        )

        memberRepository.addMember(roomId, member) { result ->
            when (result) {
                is NetworkResult.OnSuccess -> {}
                is NetworkResult.OnError -> {
                    mView?.showError(result.message)
                }
            }
            mView?.showLoading2(false)
        }
    }

    override fun deleteCurrentMember(roomId: String) {
        mView?.showLoading2(true)
        val currentUserId = UserSession.userId ?: ""
        if (currentUserId.isNotEmpty())
            memberRepository.removeMember(roomId, currentUserId) { result ->
                when (result) {
                    is NetworkResult.OnSuccess -> {}
                    is NetworkResult.OnError -> {
                        mView?.showError(result.message)
                    }
                }
                mView?.showLoading2(false)
            }
    }

    override fun removeMemberListener(roomId: String) {
        memberRepository.removeChildEventListener(roomId)
    }
}
