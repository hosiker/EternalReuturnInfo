package com.irionna.eternalreturninfo.presentation.adapter.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.irionna.eternalreturninfo.data.model.Message
import com.irionna.eternalreturninfo.databinding.ChatItemReceiverBinding
import com.irionna.eternalreturninfo.databinding.ChatItemSenderBinding
import com.irionna.eternalreturninfo.util.ChatTime
import com.irionna.eternalreturninfo.util.ProfileImage

/**
 * 채팅방 말풍선 목록.
 *  - 내 메시지는 오른쪽(초록), 상대 메시지는 왼쪽(회색, 묶음의 첫 메시지에만 얼굴).
 *  - 날짜가 바뀌는 곳 위에 날짜 구분선(오늘, 어제, 11월 9일 일요일).
 *  - 상대가 아직 읽지 않은 내 메시지에는 "1" 을 보여 준다. 읽지 않은 메시지 번호는 [setUnread] 로 한꺼번에 받는다
 *    (말풍선마다 서버 연결을 열던 방식을 대신한다).
 */
class ChatAdapter(
    private val partnerProfile: String?,
    private val onClickItem: (Int) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
    }
) {

    enum class ItemViewType { SENDER, RECEIVER }

    private var unread: Set<String> = emptySet()

    fun setUnread(ids: Set<String>) {
        if (ids == unread) return
        unread = ids
        notifyDataSetChanged()
    }

    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == ItemViewType.SENDER.ordinal) {
            SenderViewHolder(ChatItemSenderBinding.inflate(inflater, parent, false), onClickItem)
        } else {
            ReceiverViewHolder(ChatItemReceiverBinding.inflate(inflater, parent, false), onClickItem)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val prev = if (position > 0) getItem(position - 1) else null
        val now = ChatTime.millisOf(item)
        val before = prev?.let { ChatTime.millisOf(it) }
        // 첫 메시지이거나 앞 메시지와 날짜가 다르면 날짜 구분선
        val dayLabel = if (now != null && (before == null || !ChatTime.sameDay(before, now))) {
            ChatTime.dayLabel(holder.itemView.context, now)
        } else null
        val clock = now?.let { ChatTime.clock(it) } ?: ""

        if (holder is SenderViewHolder) {
            holder.bind(item, dayLabel, clock, unread.contains(item.id))
        } else if (holder is ReceiverViewHolder) {
            // 앞 메시지도 같은 상대가 보냈고 날짜 구분선이 없으면 얼굴을 숨긴다
            val showFace = prev == null || prev.sendId != item.sendId || dayLabel != null
            holder.bind(item, dayLabel, clock, showFace, partnerProfile)
        }
    }

    override fun getItemViewType(position: Int): Int {
        // 보낸 사람이 나이면 오른쪽(내 말풍선), 아니면 왼쪽(상대)
        return if (myUid == getItem(position).sendId) ItemViewType.SENDER.ordinal else ItemViewType.RECEIVER.ordinal
    }

    class SenderViewHolder(
        private val binding: ChatItemSenderBinding,
        private val onClickItem: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message, dayLabel: String?, clock: String, notRead: Boolean) = with(binding) {
            chatItemSenderDay.visibility = if (dayLabel != null) View.VISIBLE else View.GONE
            chatItemSenderDay.text = dayLabel
            chatItemSenderDate.text = clock
            chatItemSenderText.text = item.message
            chatItemSenderReadCount.visibility = if (notRead) View.VISIBLE else View.GONE
            chatItemSenderContainer.setOnClickListener { onClickItem(bindingAdapterPosition) }
        }
    }

    class ReceiverViewHolder(
        private val binding: ChatItemReceiverBinding,
        private val onClickItem: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Message, dayLabel: String?, clock: String, showFace: Boolean, profile: String?) = with(binding) {
            chatItemReceiverDay.visibility = if (dayLabel != null) View.VISIBLE else View.GONE
            chatItemReceiverDay.text = dayLabel
            chatItemRecevierDate.text = clock
            chatItemReceiverText.text = item.message
            if (showFace) {
                chatItemReceiverFace.visibility = View.VISIBLE
                ProfileImage.load(chatItemReceiverFace, profile)
            } else {
                chatItemReceiverFace.visibility = View.INVISIBLE // 자리는 유지해 말풍선이 줄을 맞춘다
            }
            chatItemReceiverContainer.setOnClickListener { onClickItem(bindingAdapterPosition) }
        }
    }
}
