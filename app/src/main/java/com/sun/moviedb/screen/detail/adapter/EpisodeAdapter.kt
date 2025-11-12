package com.sun.moviedb.screen.detail.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sun.moviedb.data.model.EpisodeModel
import com.sun.moviedb.databinding.ViewholderEpsItemBinding

class EpisodeAdapter(
    private val items: List<EpisodeModel>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.ViewHolder>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        context = parent.context
        val binding = ViewholderEpsItemBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val number = extractString(item.name)
        if (number != null) {
            holder.binding.tvEpsItem.text = number.toString()
        } else {
            holder.binding.tvEpsItem.text = item.name
        }
        holder.binding.tvEpsItem.setOnClickListener {
            onClick(item.linkM3u8)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun extractString(input: String): Int? {
        val numberString = input.filter { it.isDigit() }
        return numberString.toIntOrNull()
    }

    inner class ViewHolder(val binding: ViewholderEpsItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}

