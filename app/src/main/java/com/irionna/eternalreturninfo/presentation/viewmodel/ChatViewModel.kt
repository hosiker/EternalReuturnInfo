package com.irionna.eternalreturninfo.presentation.viewmodel

import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.irionna.eternalreturninfo.data.model.Message
import com.irionna.eternalreturninfo.util.ChatTime
import com.irionna.eternalreturninfo.presentation.repository.ChatRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.atomic.AtomicLong

class ChatViewModel(
    val repository: ChatRepository
) : ViewModel() {

    private val _list: MutableLiveData<List<Message>> = MutableLiveData()
    val list: LiveData<List<Message>> get() = _list

    private val _message: MutableLiveData<String> = MutableLiveData()
    val message: LiveData<String> get() = _message

    private val _notify: MutableLiveData<String> = MutableLiveData()
    val notify: LiveData<String> get() = _notify

    private val _scroll: MutableLiveData<String> = MutableLiveData()
    val scroll: LiveData<String> get() = _scroll

    // 메시지를 보내지 못했을 때(예: 상대가 나를 차단함) 화면에 알리기 위한 신호
    private val _sendFailed: MutableLiveData<String> = MutableLiveData()
    val sendFailed: LiveData<String> get() = _sendFailed

    private val idGenerate = AtomicLong(1L)
    private lateinit var database: DatabaseReference
    private lateinit var refDb: DatabaseReference
    private lateinit var refEventListener: ValueEventListener

    fun addItem(
        message: Message?
    ) {
        _list.value = repository.addItem(message)
    }

    fun clearList() {
        _list.value = repository.clearList()
    }

    fun getListSize(): Int {
        return repository.getListSize()
    }

    fun saveChat(receiverUid: String, authUid: String, message: String) {
        // 로그인 한 사용자 uid
        val senderUid = authUid

        // 보낸이 방
        val senderRoom = receiverUid + senderUid
        // 받는이 방
        val receiverRoom = senderUid + receiverUid

            // 메시지 저장하기: 숫자 시각(ts)과 예전과 같은 모양의 글자(time)를 함께 저장한다
            val now = System.currentTimeMillis()
            val time = ChatTime.legacyText(now)
            // et에 입력한 메시지
//            val message = binding.chatMsgEt.text.toString()
//            _message.value = message
            // 메시지 번호는 시각 + 순번이라 같은 분에 여러 번 보내도 겹치지 않는다
            val messageObject = Message(id = "$now-${idGenerate.getAndIncrement()}", message = message , sendId = senderUid, time = time, receiverId = receiverUid, readOrNot = false, whereRU = true, ts = now)


            if (message != "") {
                // 송수신 방 둘 다 저장
                database = FirebaseDatabase.getInstance().reference
                database.child("chats").child(senderRoom).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        database.child("chats").child(receiverRoom).child("messages").push()
                            .setValue(messageObject)
                    }.addOnFailureListener {
                        _sendFailed.value = "failed"
                    }

                // 메시지 전송 후 EditText 공백 처리
                // binding.chatMsgEt.setText("")
                _message.value = ""

                // 전송 버튼을 누르면 whereRU를 true로 바꿔줘 채팅리스트에 추가
                val map = HashMap<String, Any>()
                map.put("whereRU", true)
            }
    }

    fun loadChat(receiverName: String, receiverUid: String, authUid: String) {
        // 로그인 한 사용자 uid
        val senderUid = authUid

        // 보낸이 방
        val senderRoom = receiverUid + senderUid
        // 받는이 방
        val receiverRoom = senderUid + receiverUid

        var finalMessage = ""
        var finalTime = ""
        database = FirebaseDatabase.getInstance().reference
        refDb = database.child("chats").child(senderRoom).child("messages")
        refEventListener = object : ValueEventListener {
            override fun onDataChange(snapShot: DataSnapshot) {
                // 새로운 메시지 송, 수신시 최하단 화면으로 이동
//                binding.chatRecycler.scrollToPosition(messageList.size)

                val messageList = ArrayList<Message>()
                clearList()
//                messageList.clear()

                for (child in snapShot.children) {
                    val message = child.getValue(Message::class.java) ?: continue
                    messageList.add(message)
                    addItem(message)

                    // 상대가 보낸 안 읽은 메시지만 "읽음"으로 바꾼다 (이미 읽은 메시지는 다시 쓰지 않는다)
                    if (message.sendId != senderUid && message.readOrNot != true) {
                        child.ref.child("readOrNot").setValue(true)
                    }
                }
                // 채팅방에 들어왔거나 새 메시지가 오면 가장 밑으로 이동
                _scroll.value = "scroll to bottom!"

                _notify.value = "notifyDataSetChanged!"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("choco5733", error.message)
            }
        }

        // 메시지 가져오기
        refDb.addValueEventListener(refEventListener)
    }

    // 상대가 아직 읽지 않은 내 메시지 번호들 (내 말풍선 옆 "1" 표시용). 말풍선마다 연결을 열지 않고 방 하나만 듣는다.
    private val _unreadIds: MutableLiveData<Set<String>> = MutableLiveData(emptySet())
    val unreadIds: LiveData<Set<String>> get() = _unreadIds
    private var partnerRef: DatabaseReference? = null
    private var partnerListener: ValueEventListener? = null

    /** 상대 사본 방(내uid+상대uid)을 한 번만 듣고, 상대가 읽지 않은 내 메시지 번호를 알려 준다. */
    fun loadPartnerRead(receiverUid: String, authUid: String) {
        if (partnerListener != null) return
        val ref = FirebaseDatabase.getInstance().reference.child("chats").child(authUid + receiverUid).child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapShot: DataSnapshot) {
                val ids = HashSet<String>()
                for (child in snapShot.children) {
                    val m = child.getValue(Message::class.java) ?: continue
                    if (m.readOrNot == false && m.id != null) ids.add(m.id)
                }
                _unreadIds.value = ids
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("chat", error.message)
            }
        }
        partnerRef = ref
        partnerListener = listener
        ref.addValueEventListener(listener)
    }

    fun disconnectFirebase() {
        // loadChat 이 호출되기 전(변수가 아직 만들어지지 않은 상태)에 불려도 꺼지지 않게 한다.
        if (::refDb.isInitialized && ::refEventListener.isInitialized) {
            refDb.removeEventListener(refEventListener)
        }
        partnerListener?.let { partnerRef?.removeEventListener(it) }
        partnerListener = null
        partnerRef = null
    }

    // 채팅 화면이 닫히면(뒤로가기 제스처 포함) 뷰모델이 정리되면서 리스너가 해제된다.
    override fun onCleared() {
        disconnectFirebase()
        super.onCleared()
    }
}
