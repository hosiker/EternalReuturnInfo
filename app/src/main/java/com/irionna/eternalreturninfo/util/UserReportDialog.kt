package com.irionna.eternalreturninfo.util

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.irionna.eternalreturninfo.R

/**
 * 사람(이용자)을 신고하는 창. 사유를 고르고 [신고]를 누르면 Realtime Database 의 userReports 에 한 건이 추가된다.
 * 신고 내용은 서버 규칙으로 운영자만 읽을 수 있다. (이용약관 5항의 금지 내용 분류와 같은 사유를 쓴다)
 */
object UserReportDialog {
    /** 서버 규칙이 허용하는 사유 값과 화면에 보여 줄 이름 */
    private val REASONS = listOf(
        "harassment" to R.string.report_reason_harassment,
        "sexual" to R.string.report_reason_sexual,
        "minor" to R.string.report_reason_minor,
        "spam" to R.string.report_reason_spam,
        "impersonation" to R.string.report_reason_impersonation,
        "other" to R.string.report_reason_other
    )

    /** source: "chat" 또는 "post". contextId: 신고한 곳을 찾는 데 쓰는 값(글 id 등, 선택). */
    fun show(context: Context, targetUid: String?, source: String, contextId: String? = null) {
        val myUid = FirebaseAuth.getInstance().uid
        if (targetUid.isNullOrEmpty() || myUid == null || targetUid == myUid) return

        AppDialog.choose(
            context,
            title = context.getString(R.string.report_user_title),
            labels = REASONS.map { context.getString(it.second) },
            confirmText = context.getString(R.string.report_user_ok),
            danger = true
        ) { selected ->
            submit(context, myUid, targetUid, REASONS[selected].first, source, contextId)
        }
    }

    private fun submit(
        context: Context,
        myUid: String,
        targetUid: String,
        reason: String,
        source: String,
        contextId: String?
    ) {
        val report = mutableMapOf<String, Any>(
            "reporter" to myUid,
            "target" to targetUid,
            "reason" to reason,
            "date" to System.currentTimeMillis(),
            "source" to source
        )
        if (!contextId.isNullOrEmpty()) report["context"] = contextId

        FirebaseDatabase.getInstance().reference.child("userReports").push().setValue(report)
            .addOnCompleteListener {
                Toast.makeText(
                    context,
                    if (it.isSuccessful) R.string.report_user_done else R.string.report_user_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
