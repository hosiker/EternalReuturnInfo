package com.irionna.eternalreturninfo.util

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.retrofit.FBRef

/**
 * 게시글 작성자의 이름·프로필 이미지를 앱이 켜져 있는 동안 저장해 두고 다시 쓴다.
 * 카드마다 서버와 계속 연결을 열어 두던 방식(끊지 않아 쌓이던 문제)을 대신한다. 같은 작성자를 동시에 여러 번 묻지 않는다.
 */
object AuthorCache {
    class Author(val name: String?, val profilePicture: String?, val exists: Boolean)

    private val cache = HashMap<String, Author>()
    private val waiting = HashMap<String, MutableList<(Author) -> Unit>>()

    /** 저장된 값이 있으면 바로, 없으면 서버에서 한 번 받아서 [onResult] 로 알려 준다. */
    fun get(uid: String?, onResult: (Author) -> Unit) {
        if (uid.isNullOrEmpty()) {
            onResult(Author(null, null, false))
            return
        }
        cache[uid]?.let {
            onResult(it)
            return
        }
        val pending = waiting[uid]
        if (pending != null) {
            pending.add(onResult)
            return
        }
        waiting[uid] = mutableListOf(onResult)
        FBRef.userRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = if (snapshot.exists()) snapshot.getValue(ERModel::class.java) else null
                finish(uid, Author(user?.name, user?.profilePicture, user != null))
            }

            override fun onCancelled(error: DatabaseError) {
                // 실패한 값은 저장하지 않아 다음에 다시 시도한다
                waiting.remove(uid)?.forEach { it(Author(null, null, false)) }
            }
        })
    }

    private fun finish(uid: String, author: Author) {
        cache[uid] = author
        waiting.remove(uid)?.forEach { it(author) }
    }

    /** 닉네임이나 프로필을 바꾼 뒤 다시 받게 하려면 지운다. */
    fun clear() {
        cache.clear()
    }
}
