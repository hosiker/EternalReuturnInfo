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
    var avgrank :String? = null

) : Parcelable