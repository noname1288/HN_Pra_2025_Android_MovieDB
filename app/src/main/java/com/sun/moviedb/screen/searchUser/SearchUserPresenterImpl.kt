package com.sun.moviedb.screen.searchUser

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.sun.moviedb.data.model.NotificationModel
import com.sun.moviedb.data.model.User
import com.sun.moviedb.data.repository.firestore.UserRepository
import com.sun.moviedb.data.repository.rtdb.notification.NotificationOperationListener
import com.sun.moviedb.data.repository.rtdb.notification.NotificationRepository
import com.sun.moviedb.data.repository.rtdb.notification.NotificationRepositoryImpl
import com.sun.moviedb.utils.session.RoomSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchUserPresenterImpl(
    private val userRepository: UserRepository,
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job()),
    private val notificationRepository: NotificationRepository = NotificationRepositoryImpl(
        FirebaseAuth.getInstance(), FirebaseDatabase.getInstance()
    )
) : SearchUserContract.Presenter {

    private var view: SearchUserContract.View? = null
    private val TAG = "SearchUserPresenter"

    private val currentSearchableUsers = mutableListOf<User>()
    private val currentChosenUsers = mutableListOf<User>()

    override fun attachView(view: SearchUserContract.View) {
        this.view = view
        loadInitialUsers()
    }

    override fun detachView() {
        this.view = null
    }

    override fun loadInitialUsers() {
        view?.showLoading()
        mainScope.launch {
            val result = userRepository.getAllUsers()
            withContext(Dispatchers.Main) {
                view?.hideLoading()
                result.fold(
                    onSuccess = { users ->
                        currentSearchableUsers.clear()
                        currentSearchableUsers.addAll(users.filter { initialUser ->
                            currentChosenUsers.none { chosenUser -> chosenUser.id == initialUser.id }
                        })

                        if (currentSearchableUsers.isEmpty()) {
                            view?.showSearchableUsersEmpty("No users found.")
                        } else {
                            view?.hideSearchableUsersEmpty()
                        }
                        view?.displaySearchableUsers(ArrayList(currentSearchableUsers))
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error fetching initial users", exception)
                        view?.displayError("Failed to load users: ${exception.localizedMessage}")
                        view?.showSearchableUsersEmpty("Error loading users. Please try again.")
                    }
                )
            }
        }
    }

    override fun searchUsers(query: String) {
        if (query.isBlank()) {
            loadInitialUsers()
            return
        }
        if (query.length < 2 && query.isNotEmpty()) {
            view?.displaySearchableUsers(emptyList())
            view?.showSearchableUsersEmpty("Enter at least 2 characters to search.")
            return
        }

        view?.showLoading()
        mainScope.launch {
            val result = userRepository.searchUsers(query)
            withContext(Dispatchers.Main) {
                view?.hideLoading()
                result.fold(
                    onSuccess = { users ->
                        currentSearchableUsers.clear()
                        currentSearchableUsers.addAll(users.filter { searchableUser ->
                            currentChosenUsers.none { chosenUser -> chosenUser.id == searchableUser.id }
                        })

                        if (currentSearchableUsers.isEmpty()) {
                            view?.showSearchableUsersEmpty("No users found matching '$query'.")
                        } else {
                            view?.hideSearchableUsersEmpty()
                        }
                        view?.displaySearchableUsers(ArrayList(currentSearchableUsers))
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error searching users", exception)
                        view?.displayError("Failed to search users: ${exception.localizedMessage}")
                        view?.showSearchableUsersEmpty("Error searching. Please try again.")
                    }
                )
            }
        }
    }

    override fun selectUser(user: User) {
        if (currentChosenUsers.any { it.id == user.id }) return

        currentChosenUsers.add(user)

        view?.addUserToChosenList(user)
        view?.displayChosenUsers(ArrayList(currentChosenUsers))


        if (currentChosenUsers.isEmpty()) {
            view?.showChosenUsersEmpty("No users selected.")
        } else {
            view?.hideChosenUsersEmpty()
        }
        updateInviteButtonState()
    }

    override fun deselectUser(user: User) {
        val removedFromChosen = currentChosenUsers.removeAll { it.id == user.id }
        if (removedFromChosen) {
            if (!currentSearchableUsers.any { it.id == user.id }) {
                currentSearchableUsers.add(0, user)
            }


            view?.removeUserFromChosenList(user)

            view?.displayChosenUsers(ArrayList(currentChosenUsers))
            view?.displaySearchableUsers(ArrayList(currentSearchableUsers))

            if (currentChosenUsers.isEmpty()) {
                view?.showChosenUsersEmpty("No users selected.")
            } else {
                view?.hideChosenUsersEmpty()
            }
            updateInviteButtonState()
        }
    }


    override fun onInviteClicked() {
        if (currentChosenUsers.isNotEmpty()) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            var invitesSuccessfullySent = 0
            var invitesFailed = 0
            val totalInvites = currentChosenUsers.size
            currentChosenUsers.forEach { recipientUser ->
                val inviteNotification = NotificationModel.Invite(
                    title = "Lời mời mời tham gia phòng",
                    body = "${currentUser?.displayName} mời bạn tham gia phòng ${RoomSession.roomName}.",
                    createAt = System.currentTimeMillis(),
                    read = false,
                    roomId = RoomSession.roomId!!,
                    roomName = RoomSession.roomName!!,
                    senderId = currentUser?.uid ?:"",
                    senderName = currentUser?.displayName?:"",
                    senderAvatar = currentUser?.photoUrl.toString(),
                    movieName = RoomSession.roomName!!,
                    movieLink = RoomSession.movieLink!!
                )

                notificationRepository.addNotification(
                    recipientUser.id,
                    inviteNotification,
                    object : NotificationOperationListener {
                        override fun onSuccess() {
                            invitesSuccessfullySent++
                            Log.d(TAG, "Invite sent successfully to ${recipientUser.username}")
                            checkAllInvitesProcessed(totalInvites, invitesSuccessfullySent, invitesFailed)
                        }

                        override fun onError(exception: Exception) {
                            invitesFailed++
                            Log.e(TAG, "Failed to send invite to ${recipientUser.username}", exception)
                            checkAllInvitesProcessed(totalInvites, invitesSuccessfullySent, invitesFailed)
                        }
                    }
                )
            }
        }
    }

    private fun updateInviteButtonState() {
        val count = currentChosenUsers.size
        view?.updateInviteButton(count, count > 0)
    }

    private fun checkAllInvitesProcessed(total: Int, success: Int, failed: Int) {
        if (success + failed == total) {
            view?.showSendingInvitesLoading(false)
            if (failed == 0) {
                view?.showInviteSentSuccess(success)
            } else {
                view?.showInviteSentError("Sent $success invites. Failed to send $failed invites.")
            }
        }
    }
}
