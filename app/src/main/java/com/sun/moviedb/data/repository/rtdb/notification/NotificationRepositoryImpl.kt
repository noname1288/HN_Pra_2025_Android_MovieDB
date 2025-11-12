package com.sun.moviedb.data.repository.rtdb.notification

import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sun.moviedb.data.model.NotificationModel
import com.sun.moviedb.data.model.NotificationType

@OptIn(UnstableApi::class)
class NotificationRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
): NotificationRepository{

    private val notificationsNodeRef = firebaseDatabase.reference.child("notifications")
    private var activeNotificationsListener: ValueEventListener? = null
    private var currentUserNotificationsRef: DatabaseReference? = null

    companion object {
        private const val TAG = "NotificationRepoImpl"
    }

    override fun listenForUserNotifications(listener: NotificationsFetchListener) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "User not logged in. Cannot fetch notifications.")
            listener.onError(Exception("User not authenticated"))
            return
        }

        currentUserNotificationsRef = notificationsNodeRef.child(userId)

        activeNotificationsListener?.let {
            currentUserNotificationsRef?.removeEventListener(it)
            Log.d(TAG, "Removed existing notifications listener for user: $userId")
        }

        activeNotificationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<NotificationModel>()
                if (!snapshot.exists()) {
                    Log.d(TAG, "No notifications found for user: $userId")
                    listener.onNotificationsReceived(emptyList())
                    return
                }
                snapshot.children.forEach { notificationSnapshot ->
                    try {
                        val type = notificationSnapshot.child("type").getValue(String::class.java)
                        val notification = when (type) {
                            NotificationType.INVITE -> notificationSnapshot.getValue(NotificationModel.Invite::class.java)
                            NotificationType.SYSTEM -> notificationSnapshot.getValue(NotificationModel.System::class.java)
                            else -> {
                                Log.w(TAG, "Unknown notification type: $type for ID: ${notificationSnapshot.key}")
                                null
                            }
                        }
                        notification?.let { notifications.add(it) }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing notification ID: ${notificationSnapshot.key}", e)
                    }
                }
                notifications.sortByDescending { it.createAt }
                Log.d(TAG, "Fetched ${notifications.size} notifications for user: $userId")
                listener.onNotificationsReceived(notifications)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "DB error listening for notifications for user: $userId", error.toException())
                listener.onError(error.toException())
            }
        }
        currentUserNotificationsRef?.addValueEventListener(activeNotificationsListener!!)
        Log.d(TAG, "Attached notifications listener for user: $userId")
    }

    override fun removeNotificationsListener() {
        activeNotificationsListener?.let {
            currentUserNotificationsRef?.removeEventListener(it)
            Log.d(TAG, "Detached notifications listener for user: ${currentUserNotificationsRef?.key}")
        }
        activeNotificationsListener = null
        currentUserNotificationsRef = null
    }

    override fun markNotificationAsRead(notificationId: String, listener: NotificationOperationListener) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "User not logged in. Cannot mark notification as read.")
            listener.onError(Exception("User not authenticated"))
            return
        }

        if (notificationId.isEmpty()) {
            Log.w(TAG, "Notification ID is empty.")
            listener.onError(IllegalArgumentException("Notification ID cannot be empty"))
            return
        }

        notificationsNodeRef.child(userId).child(notificationId).child("read").setValue(true)
            .addOnSuccessListener {
                Log.d(TAG, "Notification marked as read: $notificationId for user: $userId")
                listener.onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to mark notification as read: $notificationId for user: $userId", exception)
                listener.onError(exception)
            }
    }

    override fun addNotification(
        targetUserId: String,
        notification: NotificationModel,
        listener: NotificationOperationListener
    ) {
        if (targetUserId.isBlank()) {
            Log.w(TAG, "Target User ID is blank. Cannot add notification.")
            listener.onError(IllegalArgumentException("Target User ID cannot be empty"))
            return
        }

        val targetUserNotificationsRef = notificationsNodeRef.child(targetUserId)
        val finalNotificationId = if (notification.id.isNotBlank()) {
            notification.id
        } else {
            targetUserNotificationsRef.push().key ?: run {
                Log.e(TAG, "Could not generate new notification ID for user: $targetUserId")
                listener.onError(Exception("Failed to generate notification ID"))
                return
            }
        }

        val notificationDataToSave = when (notification) {
            is NotificationModel.Invite -> notification.copy(id = finalNotificationId)
            is NotificationModel.System -> notification.copy(id = finalNotificationId)
            // Add other types if you have them
        }

        targetUserNotificationsRef.child(finalNotificationId).setValue(notificationDataToSave)
            .addOnSuccessListener {
                Log.d(TAG, "Notification added/updated: $finalNotificationId for target user: $targetUserId")
                listener.onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to add/update notification: $finalNotificationId for target user: $targetUserId", exception)
                listener.onError(exception)
            }
    }


}