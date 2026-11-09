package com.irionna.eternalreturninfo.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.irionna.eternalreturninfo.data.model.ChatRoomItem
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.data.model.Message
import com.irionna.eternalreturninfo.util.ChatTime

/**
 * 채팅 목록.
 * 대화방 이름은 "상대uid + 내uid" 라서 방 목록을 바로 받을 수 없다. 그래서 목록에 들어올 때
 *  1) 사용자 목록을 한 번 받고,
 *  2) 상대마다 그 방의 최근 메시지(최대 30개)를 한 번씩만 읽어 대화가 있는지 확인하고,
 *  3) 대화가 있는 방에만 실시간 연결을 열어 새 메시지가 오면 목록을 갱신한다.
 * 화면을 떠나면 [stop] 으로 모든 연결을 닫는다. (이전에는 사용자 전원에게 연결 2개씩을 열고 닫지 않았다)
 */
class ChatListViewModel : ViewModel() {

    private val _rooms = MutableLiveData<List<ChatRoomItem>>(emptyList())
    val rooms: LiveData<List<ChatRoomItem>> get() = _rooms

    /** 처음 확인이 끝났는지 (끝나기 전에는 "대화가 없어요" 안내를 보이지 않는다) */
    private val _loaded = MutableLiveData(false)
    val loaded: LiveData<Boolean> get() = _loaded

    private val db = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    private val items = HashMap<String, ChatRoomItem>() // 상대 uid → 한 줄
    private val listeners = ArrayList<Pair<Query, ValueEventListener>>()
    private var generation = 0 // stop/start 로 이전 응답을 무시하기 위한 번호

    fun start() {
        stop()
        val me = auth.currentUser?.uid ?: return
        val gen = generation
        db.child("user").get().addOnSuccessListener { snap ->
            if (gen != generation) return@addOnSuccessListener
            val users = snap.children.mapNotNull { it.getValue(ERModel::class.java) }
                .filter { !it.uid.isNullOrEmpty() && it.uid != me }
            if (users.isEmpty()) {
                _loaded.value = true
                return@addOnSuccessListener
            }
            var remaining = users.size
            for (user in users) {
                val query = db.child("chats").child(user.uid + me).child("messages").limitToLast(RECENT)
                query.get().addOnCompleteListener { task ->
                    if (gen != generation) return@addOnCompleteListener
                    val result = task.result
                    if (task.isSuccessful && result != null && result.exists()) {
                        handle(user, result, me)
                        attach(query, user, me, gen) // 대화가 있는 방만 실시간으로 듣는다
                    }
                    remaining--
                    if (remaining == 0) {
                        publish()
                        _loaded.value = true
                    }
                }
            }
        }.addOnFailureListener {
            Log.e("ChatList", "사용자 목록을 받지 못했어요: ${it.message}")
            if (gen == generation) _loaded.value = true
        }
    }

    private fun attach(query: Query, user: ERModel, me: String, gen: Int) {
        val l = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (gen != generation) return
                handle(user, snapshot, me)
                publish()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatList", error.message)
            }
        }
        listeners.add(query to l)
        query.addValueEventListener(l)
    }

    /** 방의 최근 메시지에서 마지막 메시지와 안 읽은 수를 뽑아 한 줄로 저장한다. */
    private fun handle(user: ERModel, snapshot: DataSnapshot, me: String) {
        val uid = user.uid ?: return
        val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
        val last = messages.lastOrNull()
        if (last == null || last.whereRU != true) {
            items.remove(uid)
            return
        }
        val unread = messages.count { it.sendId != me && it.readOrNot == false }
        items[uid] = ChatRoomItem(user, last.message.orEmpty(), ChatTime.millisOf(last) ?: 0L, unread)
    }

    private fun publish() {
        _rooms.value = items.values.sortedByDescending { it.lastAt }
    }

    /** 화면을 떠날 때: 모든 실시간 연결을 닫는다. */
    fun stop() {
        generation++
        listeners.forEach { (q, l) -> q.removeEventListener(l) }
        listeners.clear()
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    private companion object {
        const val RECENT = 30
    }
}
