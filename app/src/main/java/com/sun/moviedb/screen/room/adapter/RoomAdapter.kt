package com.sun.moviedb.screen.room.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sun.moviedb.R
import com.sun.moviedb.data.model.Member
import com.sun.moviedb.databinding.ViewholderMemberItemBinding
import com.sun.moviedb.utils.session.UserSession

class RoomAdapter(
    private val onRemoveMember: (Member) -> Unit
) : RecyclerView.Adapter<RoomAdapter.ViewHolder>() {
    private lateinit var context: Context
    private val items = mutableListOf<Member>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(newList: List<Member>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeItem(member: Member) {
        val position = items.indexOfFirst { it.memberId == member.memberId }
        if (position != -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        context = parent.context
        val binding =
            ViewholderMemberItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvMemberName.text = item.memberName
        holder.binding.tvRole.text = if (item.host) "Chủ phòng" else "Khách"
        holder.binding.btnRemoveMember.visibility =
            if (item.host || (item.memberId == UserSession.userId)) ViewGroup.GONE else ViewGroup.VISIBLE
        if (item.linkAvatar.isNotEmpty()) {
            Glide.with(context)
                .load(item.linkAvatar)
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.binding.imgAvtMember)
        }

        holder.binding.btnRemoveMember.setOnClickListener {
            onRemoveMember(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val binding: ViewholderMemberItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}