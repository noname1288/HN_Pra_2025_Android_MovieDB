package com.sun.moviedb.screen.room

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.sun.moviedb.data.model.Member
import com.sun.moviedb.databinding.FragmentRoomBinding
import com.sun.moviedb.screen.room.adapter.RoomAdapter
import com.sun.moviedb.screen.watchMovie.WatchMovieContract
import com.sun.moviedb.utils.base.BaseFragment
import com.sun.moviedb.utils.session.RoomSession

class RoomFragment : BaseFragment<FragmentRoomBinding>(), RoomContract.View {

    private lateinit var roomAdapter: RoomAdapter
    private val memberList = mutableListOf<Member>()
    private val roomId = RoomSession.roomId ?: ""
    private var watchPresenter: WatchMovieContract.Presenter? = null
    private lateinit var presenter: RoomContract.Presenter

    fun setWatchPresenter(presenter: WatchMovieContract.Presenter) {
        this.watchPresenter = presenter
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentRoomBinding {
        return FragmentRoomBinding.inflate(inflater, container, false)
    }

    override fun initData() {
        super.initData()
        presenter = RoomPresenter()
        presenter.attachView(this)
        setupUI()

        onLeaveRoomButtonClicked()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
    }

    private fun setupUI() {
        if (watchPresenter == null)
            throw Exception("Presenter is null, please set it before using RoomFragment")

        roomAdapter = RoomAdapter { choosenMember ->
            if (roomId.isEmpty())
                throw Exception("Room Id is empty, please set it before removing member in HomeFragment")
//            watchPresenter!!.removeChosenMember(roomId, choosenMember)
            roomAdapter.removeItem(choosenMember)
        }

        /*
        * get current list of members if exists
        * */
        roomAdapter.setItems(watchPresenter!!.getCachedMembers())

        binding.rvListMember.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL, false
        )
        binding.rvListMember.adapter = roomAdapter

    }

    fun updateMembers(members: List<Member>) {
        memberList.clear()
        memberList.addAll(members)
        roomAdapter.setItems(memberList)
    }

    override fun showLoading(isLoading: Boolean) {}

    override fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun onLeaveRoomButtonClicked() {
        binding.btnOutRoom.setOnClickListener {
            requireActivity().setResult(
                Activity.RESULT_OK,
                Intent()
                    .putExtra(HAS_ROOM, true)
                    .putExtra(MESSAGE_AFTER_LEFT_ROOM, "Bạn đã rời khỏi phòng")
            )
            requireActivity().finish()
        }
    }

    companion object {
        const val HAS_ROOM = "left_room"
        const val MESSAGE_AFTER_LEFT_ROOM = "message_after_left_room"
    }

}

