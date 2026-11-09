package com.irionna.eternalreturninfo.data.model

/** 채팅 목록의 한 줄: 상대, 마지막 메시지, 마지막 시각(밀리초), 안 읽은 메시지 수(최근 30개 안에서) */
data class ChatRoomItem(
    val user: ERModel,
    val lastMessage: String,
    val lastAt: Long,
    val unread: Int
)
