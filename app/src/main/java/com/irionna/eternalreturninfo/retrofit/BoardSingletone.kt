package com.irionna.eternalreturninfo.retrofit

import android.util.Log
import androidx.annotation.Keep
import com.irionna.eternalreturninfo.data.model.ERModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

@Keep object BoardSingletone {

    private var loginUser: ERModel = ERModel()
    private var seasonID: String = ""

    fun LoginUser(): ERModel {
        return loginUser
    }

    fun seasonID(): String{
        return seasonID
    }

    fun manager(): ERModel {
        return ERModel(uid="j2JTWMpZdEUWxao4mYoYC3Acheg1")
    }

    fun Login(){

        val uid = FirebaseAuth.getInstance().uid

        if (uid != null) {
            FBRef.userRef.child(uid).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {
//                        val user = snapshot.getValue<ERModel>()
                        val user = snapshot.getValue(ERModel::class.java)
                        if (user != null) {
                            loginUser = user
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // 파이어베이스 규칙 적용 후, 로그아웃 시 간헐적으로 팅김 방지용 코드입니다!
                    Log.d("choco5732 BoardSingletone", error.message)
                }
            })
        }

        FBRef.seasonRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
//                    val season = snapshot.getValue<String>()
                    val season  = snapshot.getValue(String::class.java)
                    if (season != null) {
                        seasonID = season
                        Log.d("seasonID", season)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                /**
                 * 회원탈퇴 시 런타임에러 발생해서
                 * 투두 코드 삭제
                 */
                Log.d("choco5732", "시즌ID 불러오기 실패")

            }
        })

    }


}