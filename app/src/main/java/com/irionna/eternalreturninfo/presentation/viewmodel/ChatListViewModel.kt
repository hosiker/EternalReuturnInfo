package com.irionna.eternalreturninfo.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.data.model.Message
import com.irionna.eternalreturninfo.presentation.repository.ChatListRepository
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.Firebase

class ChatListViewModel(
    val repository: ChatListRepository
) : ViewModel() {
    private val _list: MutableLiveData<List<ERModel>> = MutableLiveData()
    val list: LiveData<List<ERModel>> get() = _list

    private val _profilePictureUrl: MutableLiveData<String> = MutableLiveData()
    val profilePictureUrl: LiveData<String> get() = _profilePictureUrl

    private val _profileName: MutableLiveData<String?> = MutableLiveData()
    val profileName: LiveData<String?> get() = _profileName


    private lateinit var database: DatabaseReference
    private var auth = Firebase.auth
    private lateinit var refEventListener: ValueEventListener
    private lateinit var refDb: DatabaseReference

    fun addUser(
        item: ERModel?
    ) {
        _list.value = repository.addUser(item)
    }

    fun clearList() {
        _list.value = repository.clearList()
    }

    fun modifyItemForCallBack(position: Int, message: String, time: String) {
        _list.value = repository.modifyItemForCallBack(position, message, time)
    }

    fun modifyItemForChatList(item: ERModel?) {
        _list.value = repository.modifyItemForChatList(item)
    }

    fun addChatList() {
        database = Firebase.database.reference
        refDb = database.child("user")
        refEventListener = object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // 리스트 초기화 for 회원가입시 리스트 중복추가 문제 해결
                clearList()
                var senderRoom = ""
                var receiverRoom = ""

                for (child in snapshot.children) {
                    val currentUser = child.getValue(ERModel::class.java)

                    senderRoom = currentUser?.uid + auth.currentUser?.uid
                    var message = Message()

                    database.child("chats").child(senderRoom).child("messages")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (child in snapshot.children) {
                                    message = child.getValue(Message::class.java)!!
                                }
                                if (message.whereRU == true) {
                                    addUser(
                                        currentUser?.copy(
                                            msg = message.message,
                                            time = message.time,
                                            readOrNot = message.readOrNot
                                        )
                                    )
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("choco5733 in chatList", error.message)
                            }

                        })

                    database.child("chats").child(senderRoom).child("messages")
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {

                                for (child in snapshot.children) {
                                    message = child.getValue(Message::class.java)!!
                                }
                                Log.d("choco5733 in msg", "$message")


                                modifyItemForChatList(
                                    currentUser?.copy(
                                        msg = message.message,
                                        time = message.time,
                                        readOrNot = message.readOrNot
                                    )
                                )
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("choco5733", error.message)
                            }
                        })

                    if (auth.uid == currentUser?.uid) {
                        _profileName.value = currentUser?.name
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("choco5733", error.message)
            }

        }
        refDb.addValueEventListener(refEventListener)
    }

    fun disconnectFirebase() {
        refDb.removeEventListener(refEventListener)
    }

    fun loadProfilePicture()  {
        database = Firebase.database.reference
        database.child("user").get().addOnSuccessListener {
            for(child in it.children) {
                val currentUser = child.getValue(ERModel::class.java)
                if (currentUser?.uid == auth.uid) {
                    _profilePictureUrl.value = currentUser?.profilePicture.toString()
                }
            }
        }
    }
}