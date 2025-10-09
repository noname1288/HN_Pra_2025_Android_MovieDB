package com.sun.moviedb.screen.notification

import android.util.Log
import com.sun.moviedb.data.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.sun.moviedb.data.model.Member
import com.sun.moviedb.data.repository.rtdb.member.MemberRepository
import com.sun.moviedb.data.repository.rtdb.member.MemberRepositoryImpl
import com.sun.moviedb.data.repository.rtdb.notification.NotificationOperationListener
import com.sun.moviedb.data.repository.rtdb.notification.NotificationRepository
import com.sun.moviedb.data.repository.rtdb.notification.NotificationRepositoryImpl
import com.sun.moviedb.data.repository.rtdb.notification.NotificationsFetchListener
import com.sun.moviedb.data.repository.source.remote.NetworkResult
import com.sun.moviedb.utils.session.UserSession
import kotlin.String

class NotificationPresenter(
    private val notificationRepository: NotificationRepository =
        NotificationRepositoryImpl(FirebaseAuth.getInstance(), FirebaseDatabase.getInstance()),
    private val memberRepository: MemberRepository = MemberRepositoryImpl.getInstance()
) : NotificationContract.Presenter {

    private var view: NotificationContract.View? = null
    private val TAG = "NotificationPresenter"

    override fun attachView(view: NotificationContract.View) {
        this.view = view
    }

    override fun detachView() {
        this.view = null
        notificationRepository.removeNotificationsListener()
        Log.d(TAG, "Notification listener removed on detachView")
    }

    override fun loadNotifications() {
        view?.showLoading(true)
        notificationRepository.listenForUserNotifications(object : NotificationsFetchListener {
            override fun onNotificationsReceived(notifications: List<NotificationModel>) {
                view?.showLoading(false)
                if (notifications.isEmpty()) {
                    view?.showEmptyNotifications()
                } else {
                    view?.displayNotifications(notifications)
                }
            }

            override fun onError(exception: Exception) {
                view?.showLoading(false)
                Log.e(TAG, "Error loading notifications", exception)
                view?.showError("Failed to load notifications: ${exception.message}")
            }
        })
    }

    override fun markNotificationAsRead(notificationId: String) {
        if (notificationId.isEmpty()) {
            view?.showGenericError("Invalid notification ID.")
            return
        }

        notificationRepository.markNotificationAsRead(notificationId, object :
            NotificationOperationListener {
            override fun onSuccess() {
                Log.d(TAG, "Notification $notificationId marked as read successfully.")
                view?.showNotificationMarkedAsReadSuccess()
            }

            override fun onError(exception: Exception) {
                Log.e(TAG, "Error marking notification $notificationId as read", exception)
                view?.showGenericError("Failed to mark notification as read: ${exception.message}")
            }
        })
    }

    override fun addCurrentUser(roomId: String) {
        val userId = UserSession.userId

        if (userId.isNullOrEmpty()) {
            throw Exception("Can't find current user")
        } else {
            val currentUser = Member(
                userId,
                UserSession.userName ?: "",
                UserSession.linkAvatar ?: "",
                System.currentTimeMillis(),
                false
            )

            memberRepository.addMember(roomId, currentUser) { result ->
                when (result) {
                    is NetworkResult.OnError -> {
                        view?.showError(result.message)
                    }

                    is NetworkResult.OnSuccess<*> -> {//nothing}
                    }

                }
            }

        }
    }
}
