package com.irionna.eternalreturninfo.presentation.adapter.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.ChatRoomItem
import com.irionna.eternalreturninfo.databinding.ChatListItemBinding
import com.irionna.eternalreturninfo.util.ChatTime
import com.irionna.eternalreturninfo.util.ProfileImage

/** 채팅 목록: 얼굴, 이름, 마지막 시각(방금·12분 전·어제), 마지막 메시지, 안 읽은 수 배지. 안 읽은 대화는 메시지 글씨가 밝다. */
class ChatListAdapter(
    private val onClickItem: (ChatRoomItem) -> Unit
) : ListAdapter<ChatRoomItem, ChatListAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<ChatRoomItem>() {
        override fun areItemsTheSame(a: ChatRoomItem, b: ChatRoomItem) = a.user.uid == b.user.uid
        override fun areContentsTheSame(a: ChatRoomItem, b: ChatRoomItem) = a == b
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ChatListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position), onClickItem)

    class ViewHolder(private val binding: ChatListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatRoomItem, onClick: (ChatRoomItem) -> Unit) = with(binding) {
            val ctx = root.context
            chatListName.text = item.user.name
            ProfileImage.load(chatListProfilePicture, item.user.profilePicture)
            chatListMsg.text = item.lastMessage
            chatListDate.text = ChatTime.listLabel(ctx, item.lastAt)

            val unread = item.unread > 0
            chatListMsg.setTextColor(ContextCompat.getColor(ctx, if (unread) R.color.white else R.color.text_secondary))
            chatListNewMsg.visibility = if (unread) View.VISIBLE else View.GONE
            chatListNewMsg.text = if (item.unread >= 30) ctx.getString(R.string.chat_list_unread_many, 30) else item.unread.toString()

            chatListContainer.setOnClickListener { onClick(item) }
        }
    }
}
