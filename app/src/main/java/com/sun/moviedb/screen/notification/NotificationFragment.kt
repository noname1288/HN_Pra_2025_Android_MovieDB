package com.sun.moviedb.screen.notification

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.sun.moviedb.data.model.Member
import com.sun.moviedb.data.model.NotificationModel
import com.sun.moviedb.data.repository.source.remote.NetworkResult
import com.sun.moviedb.databinding.FragmentNotificationBinding
import com.sun.moviedb.screen.notification.adapter.NotificationAdapter
import com.sun.moviedb.screen.watchMovie.WatchMovieActivity
import com.sun.moviedb.utils.AppLocator
import com.sun.moviedb.utils.base.BaseFragment
import com.sun.moviedb.utils.session.UserSession

class NotificationFragment : BaseFragment<FragmentNotificationBinding>(), NotificationContract.View {

    private lateinit var presenter: NotificationContract.Presenter
    private lateinit var adapter: NotificationAdapter
    private val TAG = "NotificationFragment"

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNotificationBinding {
        return FragmentNotificationBinding.inflate(inflater, container, false)
    }

    override fun initData() {
        super.initData()
        presenter = NotificationPresenter()
        presenter.attachView(this)
        setupUI()
        presenter.loadNotifications()
    }

    private fun setupUI() {
        setupStatusBarPadding()

        adapter = NotificationAdapter { notificationModel ->
            Log.d(TAG, "Adapter item clicked: ID = ${notificationModel.id}, Type = ${notificationModel.type}")
            presenter.markNotificationAsRead(notificationModel.id)

            if (notificationModel is NotificationModel.Invite) {
                // add current member to chosen room
                presenter.addCurrentUser(notificationModel.roomId)

                Toast.makeText(requireContext(), "Clicked on invite: ${notificationModel.roomName} ${notificationModel.movieLink} ${notificationModel.roomId}", Toast.LENGTH_SHORT).show()
                navigateToWatch(notificationModel.movieLink,notificationModel.roomId)
            } else if (notificationModel is NotificationModel.System) {
                Toast.makeText(requireContext(), "Clicked on system notification: ${notificationModel.title}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvListNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListNotifications.adapter = adapter

        binding.btnJoinRoom1.setOnClickListener {
            val input = binding.editTextText.text.toString()
            Log.d(TAG, "Room ID input: $input")
            if (input.isNotEmpty()) {
                navigateToWatch(input,"")
                binding.editTextText.text.clear()
            } else {
                binding.editTextText.error = "Please enter a room ID"
            }
        }
    }

    override fun displayNotifications(notifications: List<NotificationModel>) {
        Log.d(TAG, "Displaying ${notifications.size} notifications")
        val isEmpty = notifications.isEmpty()
        binding.rvListNotifications.visibility = if (isEmpty) View.GONE else View.VISIBLE
        adapter.submitList(notifications)
    }

    override fun showEmptyNotifications() {
        Log.d(TAG, "No notifications to display.")
        binding.rvListNotifications.visibility = View.GONE
    }

    override fun showNotificationMarkedAsReadSuccess() {
        Toast.makeText(requireContext(), "Notification marked as read.", Toast.LENGTH_SHORT).show()
    }

    override fun showGenericError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun showLoading(isLoading: Boolean) {
        binding.progressBar4.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.rvListNotifications.visibility = View.GONE
        } else {
            if (adapter.currentList.isNotEmpty()) {
                binding.rvListNotifications.visibility = View.VISIBLE
            }
        }
    }

    override fun showError(message: String) {
        binding.progressBar4.visibility = View.GONE
        binding.rvListNotifications.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        Log.d(TAG, "onDestroyView called, presenter detached.")
    }

    private fun navigateToWatch(movieLink:String, roomId: String) {
        val intent = Intent(requireContext(), WatchMovieActivity::class.java).apply {
            putExtra(ARG_M3U8_LINK, movieLink)
            putExtra(ARG_ROOM_ID, roomId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun setupStatusBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun goRoom(roomId: String) {
        val member = Member(
            memberId = UserSession.userId ?: "knot",
            memberName = UserSession.userName ?: "Unknown",
            linkAvatar = UserSession.linkAvatar ?: "",
            joinAt = System.currentTimeMillis(),
            isHost = false
        )

        AppLocator.memberRepository.addMember(roomId, member) { result ->
            when (result) {
                is NetworkResult.OnSuccess -> {
                    Toast.makeText(
                        requireContext(),
                        "Member added successfully ${member.memberName}; Room: $roomId",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is NetworkResult.OnError -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val ARG_M3U8_LINK = "m3u8_link"
        const val ARG_ROOM_ID = "room_id"
    }
}
