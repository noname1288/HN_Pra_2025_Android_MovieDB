package com.sun.moviedb.screen.notification

import com.sun.moviedb.data.model.NotificationModel
import com.sun.moviedb.utils.base.BasePresenter
import com.sun.moviedb.utils.base.BaseView

interface NotificationContract {

    interface View : BaseView {
        fun displayNotifications(notifications: List<NotificationModel>)
        fun showEmptyNotifications()
        fun showNotificationMarkedAsReadSuccess()
        fun showGenericError(message: String)
    }

    interface Presenter : BasePresenter<View> {
        fun loadNotifications()
        fun markNotificationAsRead(notificationId: String)
        // fun addDummyNotification()
        fun addCurrentUser(roomId: String)
    }
}
