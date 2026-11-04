package com.irionna.eternalreturninfo.presentation.viewmodel

import android.os.Build
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.irionna.eternalreturninfo.data.model.Message
import com.irionna.eternalreturninfo.presentation.repository.ChatRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

            // 메시지 저장하기
            val time = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 a hh시 mm분"))
            } else {
                TODO("VERSION.SDK_INT < O")
            }
            // et에 입력한 메시지
//            val message = binding.chatMsgEt.text.toString()
//            _message.value = message
            val messageObject = Message(id = "${idGenerate.getAndIncrement()}" + time, message = message , sendId = senderUid, time = time, receiverId = receiverUid, readOrNot = false, whereRU = true)


            if (message != "") {
                // 송수신 방 둘 다 저장
                database = FirebaseDatabase.getInstance().reference
                database.child("chats").child(senderRoom).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        database.child("chats").child(receiverRoom).child("messages").push()
                            .setValue(messageObject)
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
                    val message = child.getValue(Message::class.java)
                    var readOrNot: Boolean = false

                    messageList.add(message!!)
                    addItem(message)

                    // 채팅방 들어왔을시 가장 밑으로 이동
//                    binding.chatRecycler.scrollToPosition(messageList.size - 1)
                    _scroll.value = "scroll to bottom!"

                    // 마지막으로 읽은 메시지와 시간을 채팅목록 화면에 주기위한 코드
                    finalMessage = messageList.last().message.toString()
                    finalTime = messageList.last().time.toString()

                    // 가져왔을 시 readOrNot을 true로 변경
                    val map = HashMap<String, Any>()
                    map.put("readOrNot", true)

                    val key = child.key
                    database.child("chats").child(senderRoom)
                        .child("messages").child("$key").updateChildren(map)
                }

                _notify.value = "notifyDataSetChanged!"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("choco5733", error.message)
            }
        }

        // 메시지 가져오기
        refDb.addValueEventListener(refEventListener)
    }

    fun disconnectFirebase() {
        refDb.removeEventListener(refEventListener)
    }
}
