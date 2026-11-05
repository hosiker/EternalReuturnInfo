package com.irionna.eternalreturninfo.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * 내가 차단한 사용자 목록 (Realtime Database: blocks/내uid/차단한사람uid = true).
 *
 * - 로그인한 뒤 [start] 를 부르면 목록을 실시간으로 따라간다. 로그아웃/탈퇴 때는 [stop] 을 부른다.
 * - 화면들은 [isBlocked] 로 숨길 사람을 가리고, [blocked] 를 관찰해서 목록이 바뀌면 다시 그린다.
 * - 차단은 서버 규칙으로도 강제된다. 상대가 나를 차단했거나 내가 상대를 차단한 채팅방에는 새 메시지를 쓸 수 없다.
 */
object BlockedUsers {
    private const val TAG = "BlockedUsers"

    private val _blocked = MutableLiveData<Set<String>>()

    /** 첫 로딩이 끝난 뒤부터 값이 있고, 차단/해제 때마다 새 값이 온다. */
    val blocked: LiveData<Set<String>> get() = _blocked

    @Volatile
    private var current: Set<String> = emptySet()
    private var ref: DatabaseReference? = null
    private var listener: ValueEventListener? = null

    fun isBlocked(uid: String?): Boolean = !uid.isNullOrEmpty() && uid in current

    /** 현재 로그인한 사용자의 차단 목록을 따라가기 시작한다. */
    fun start() {
        detach()
        val myUid = FirebaseAuth.getInstance().uid ?: return
        val newRef = FirebaseDatabase.getInstance().reference.child("blocks").child(myUid)
        val newListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                current = snapshot.children.mapNotNull { it.key }.toSet()
                _blocked.value = current
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "차단 목록을 읽지 못했습니다: ${error.message}")
            }
        }
        newRef.addValueEventListener(newListener)
        ref = newRef
        listener = newListener
    }

    /** 로그아웃이나 회원탈퇴 때: 따라가기를 멈추고 목록을 비운다. */
    fun stop() {
        detach()
        current = emptySet()
        _blocked.value = emptySet()
    }

    fun block(targetUid: String, onResult: (Boolean) -> Unit = {}) {
        val myUid = FirebaseAuth.getInstance().uid
        if (myUid == null || targetUid.isEmpty() || targetUid == myUid) {
            onResult(false)
            return
        }
        FirebaseDatabase.getInstance().reference.child("blocks").child(myUid).child(targetUid)
            .setValue(true)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun unblock(targetUid: String, onResult: (Boolean) -> Unit = {}) {
        val myUid = FirebaseAuth.getInstance().uid
        if (myUid == null || targetUid.isEmpty()) {
            onResult(false)
            return
        }
        FirebaseDatabase.getInstance().reference.child("blocks").child(myUid).child(targetUid)
            .removeValue()
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    private fun detach() {
        val r = ref
        val l = listener
        if (r != null && l != null) r.removeEventListener(l)
        ref = null
        listener = null
    }
}
