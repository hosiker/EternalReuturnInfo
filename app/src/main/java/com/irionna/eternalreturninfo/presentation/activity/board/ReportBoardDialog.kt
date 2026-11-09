package com.irionna.eternalreturninfo.presentation.activity.board

import android.content.Context
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.util.AppDialog

/** 글 신고 확인창. */
class ReportBoardDialog(private val context: Context, private val dialogListener: DialogListener) {
    fun show() {
        AppDialog.confirm(
            context,
            title = context.getString(R.string.dlg_report_post_title),
            message = context.getString(R.string.dlg_report_post_message),
            confirmText = context.getString(R.string.dlg_report_ok),
            danger = true
        ) { dialogListener.onOKButtonClicked() }
    }
}
