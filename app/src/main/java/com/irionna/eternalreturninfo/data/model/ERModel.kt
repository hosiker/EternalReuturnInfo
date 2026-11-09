package com.irionna.eternalreturninfo.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

//import kotlinx.android.parcel.Parcelize

@Parcelize
@Keep
data class ERModel (
    val id: Long? = null,
    var server: String? = null,
    var gender: String? = null,
    var tier: String? = null,
    var most: String? = null,
    val uid: String? = null,
    val email: String? = null,
    var name: String? = null,
    var msg: String? = null,
    val profilePicture: String? = null,
    var readOrNot: Boolean? = null,
    var time: String? = null,
    var winrate :String? = null,
    var avgrank :String? = null,
    // 회원탈퇴한 사용자의 표시용 노드(묘비)일 때 true
    val withdrawn: Boolean? = null,
    // 듀오 모집글: 최근 5경기 순위("1,4,2,7,3"), 티어를 전적에서 자동으로 가져왔는지, 올린 시각(밀리초 문자열)
    var recent: String? = null,
    var tierAuto: Boolean? = null,
    var timestamp: String? = null

) : Parcelable