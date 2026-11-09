package com.irionna.eternalreturninfo.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoModel(
    val id: String?,
    val thumbnail: String?,
    val title: String?,
    val url: String?,
    val isShort: Boolean = false
):Parcelable