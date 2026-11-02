package com.irionna.eternalreturninfo.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.android.parcel.Parcelize

@Parcelize
@Keep
data class BoardModel (
    val id: String = "",
    val category:String = "",
    val title: String = "",
    val content: String = "",
    val author: String? = "",
    val date: Long = 0,
    val comments: Map<String, CommentModel> = mapOf(),
    val views: Int = 0,
    val report: Map<String, ReportModel> = mapOf()
) : Parcelable

@Parcelize
@Keep
data class CommentModel(
    val id: String = "",
    val author: String? = "",
    val content: String = "",
    val date: Long = 0,
    val report: Map<String, ReportModel> = mapOf()
) : Parcelable

@Parcelize
@Keep
data class ReportModel(
    val uid: String = "",
    // 신고한 시각(ms). 탈퇴한 작성자의 신고된 글을 보관하는 기간(3개월)을 셀 때 쓴다. 옛 신고는 0.
    val date: Long = 0
) : Parcelable

@Parcelize
@Keep
data class ReportCommentModel(
    val uid: String = "",
    val key: String = ""
) : Parcelable


