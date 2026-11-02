package com.irionna.eternalreturninfo.presentation.adapter.chat

import com.irionna.eternalreturninfo.util.ProfileImage
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.databinding.ChatListItemBinding

class ChatListAdapter(
    private val onClickItem: (Int, ERModel) -> Unit
) : ListAdapter<ERModel, ChatListAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<ERModel>() {
        override fun areItemsTheSame(
            oldItem: ERModel,
            newItem: ERModel
        ): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(
            oldItem: ERModel,
            newItem: ERModel
        ): Boolean {
            return oldItem == newItem
        }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ChatListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onClickItem
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(
        private val binding: ChatListItemBinding,
        private val onClickItem: (Int, ERModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ERModel) = with(binding) {

            chatListName.text = item.name
            ProfileImage.load(chatListProfilePicture, item.profilePicture)
            chatListMsg.text = item.msg
            chatListDate.text = item.time

            chatListNewMsg.isVisible = item.readOrNot == false

            chatListContainer.setOnClickListener {
                onClickItem(
                    adapterPosition,
                    item
                )
            }
        }
    }
}