package com.sun.moviedb.screen.notification.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.sun.moviedb.R
import com.sun.moviedb.data.model.NotificationModel
import com.sun.moviedb.databinding.ViewholderNotificationItemBinding
import com.sun.moviedb.utils.TimeUtils

class NotificationAdapter(
    private val onItemClicked: (notification: NotificationModel) -> Unit
) : ListAdapter<NotificationModel, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    private lateinit var context: Context

    companion object {
        private const val VIEW_TYPE_INVITE = 1
        private const val VIEW_TYPE_SYSTEM = 2
        private const val TAG = "NotificationAdapter"
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationModel.Invite -> VIEW_TYPE_INVITE
            is NotificationModel.System -> VIEW_TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(context)
        val binding = ViewholderNotificationItemBinding.inflate(inflater, parent, false)

        return when (viewType) {
            VIEW_TYPE_INVITE -> InviteViewHolder(binding, onItemClicked)
            VIEW_TYPE_SYSTEM -> SystemViewHolder(binding, onItemClicked)
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is InviteViewHolder -> holder.bind(item as NotificationModel.Invite)
            is SystemViewHolder -> holder.bind(item as NotificationModel.System)
        }
    }

    class InviteViewHolder(
        private val binding: ViewholderNotificationItemBinding,
        private val onItemClicked: (notification: NotificationModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationModel.Invite) {
            binding.tvTitleItem.text = item.title
            binding.tvBody.text = item.body
            binding.tvDuration.text = TimeUtils.getTimeAgo(item.createAt)

            Glide.with(itemView.context)
                .load(R.drawable.icons8_movie_48)
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .into(binding.imgIconItem)

            if (item.senderAvatar.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(item.senderAvatar)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .apply(RequestOptions.circleCropTransform())
                    .into(binding.imgNotiItem)
            } else {
                binding.imgNotiItem.setImageResource(R.mipmap.ic_launcher_round)
            }

            binding.imgRedDot.visibility = if (item.read) View.GONE else View.VISIBLE

            binding.root.setOnClickListener {
                Log.d(TAG, "Invite item clicked: ${item.id}")
                onItemClicked(item)
            }
        }
    }

    class SystemViewHolder(
        private val binding: ViewholderNotificationItemBinding,
        private val onItemClicked: (notification: NotificationModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationModel.System) {
            binding.tvTitleItem.text = item.title
            binding.tvBody.text = item.body
            binding.tvDuration.text = TimeUtils.getTimeAgo(item.createAt)

            Glide.with(itemView.context)
                .load(R.drawable.icons8_speaker_48)
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .into(binding.imgIconItem)

            Glide.with(itemView.context)
                .load(R.drawable.ic_launcher_foreground)
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.imgNotiItem)

            binding.imgRedDot.visibility = if (item.read) View.GONE else View.VISIBLE

            binding.root.setOnClickListener {
                Log.d(TAG, "System item clicked: ${item.id}")
                onItemClicked(item)
            }
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationModel>() {
    override fun areItemsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: NotificationModel, newItem: NotificationModel): Boolean {
        return oldItem == newItem
    }
}
