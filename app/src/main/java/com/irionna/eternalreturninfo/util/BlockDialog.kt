package com.irionna.eternalreturninfo.util

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.repository.BlockedUsers

/** "OO님을 차단할까요?" 확인창. 게시글, 댓글, 채팅 어디에서든 같은 문구로 쓴다. */
object BlockDialog {
    fun show(
        context: Context,
        targetUid: String?,
        targetName: String?,
        onBlocked: () -> Unit = {}
    ) {
        if (targetUid.isNullOrEmpty() || targetUid == FirebaseAuth.getInstance().uid) return
        val name = targetName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.blocked_unknown_user)

        AppDialog.confirm(
            context,
            title = context.getString(R.string.block_confirm_title, name),
            message = context.getString(R.string.block_confirm_message),
            confirmText = context.getString(R.string.block_confirm_ok),
            danger = true
        ) {
            BlockedUsers.block(targetUid) { ok ->
                if (ok) {
                    Toast.makeText(context, context.getString(R.string.block_done, name), Toast.LENGTH_SHORT).show()
                    onBlocked()
                } else {
                    Toast.makeText(context, R.string.block_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
