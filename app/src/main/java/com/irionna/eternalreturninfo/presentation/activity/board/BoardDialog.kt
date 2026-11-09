package com.irionna.eternalreturninfo.presentation.activity.board

import android.content.Context
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.util.AppDialog

interface DialogListener {
    fun onOKButtonClicked()
}

/** "OO님과 1:1 채팅을 하시겠습니까?" 확인창. */
class BoardDialog(private val context: Context, val userName: String, private val dialogListener: DialogListener) {
    fun show() {
        AppDialog.confirm(
            context,
            title = context.getString(R.string.dlg_chat_title),
            message = context.getString(R.string.dlg_chat_message, userName),
            confirmText = context.getString(R.string.dlg_chat_ok)
        ) { dialogListener.onOKButtonClicked() }
    }
}
