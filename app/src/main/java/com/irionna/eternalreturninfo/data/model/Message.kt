package com.irionna.eternalreturninfo.data.model

import androidx.annotation.Keep

@Keep data class Message(
    val id: String? = null,
    var message: String? = null,
    var sendId: String? = null,
    var receiverId: String? = null,
    var time: String? = null,
    var readOrNot: Boolean? = null,
    var whereRU: Boolean? = null,
    // 보낸 시각(밀리초). 옛 메시지에는 없고 time 글자만 있다.
    val ts: Long? = null
) {
    constructor() : this("", "", "", "", "",null, null)
}
