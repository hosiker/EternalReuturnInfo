package com.irionna.eternalreturninfo.util

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.AppDialogBinding

/**
 * 앱 전체에서 같이 쓰는 가운데 카드 창. 확인·위험한 동작·목록 선택·입력창이 모두 같은 모양이다.
 *  - 일반 동작은 초록 버튼, 신고·삭제·탈퇴·나가기 같은 위험한 동작은 빨간 버튼 ([danger])
 *  - 버튼 이름은 "예/아니오" 대신 동작을 말한다 (채팅하기, 신고, 삭제)
 */
object AppDialog {

    /** 제목 + 설명 + [취소 | 확인]. */
    fun confirm(
        context: Context,
        title: String,
        message: String? = null,
        confirmText: String,
        cancelText: String = context.getString(R.string.block_cancel),
        danger: Boolean = false,
        onConfirm: () -> Unit
    ): Dialog = custom(context, title, message, null, confirmText, cancelText, danger) { dialog ->
        dialog.dismiss()
        onConfirm()
    }

    /**
     * [content] 를 넣을 수 있는 창. 확인을 누르면 [onConfirm] 이 불리고, 창을 닫을지는 호출한 쪽이 정한다
     * (입력이 잘못됐을 때 닫지 않으려고).
     */
    fun custom(
        context: Context,
        title: String,
        message: String? = null,
        content: View? = null,
        confirmText: String,
        cancelText: String = context.getString(R.string.block_cancel),
        danger: Boolean = false,
        onConfirm: (Dialog) -> Unit
    ): Dialog {
        val b = AppDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context, R.style.AppDialogTheme)
        dialog.setContentView(b.root)

        b.dlgTitle.text = title
        if (message.isNullOrBlank()) b.dlgMessage.visibility = View.GONE else b.dlgMessage.text = message
        if (content != null) {
            (content.parent as? ViewGroup)?.removeView(content)
            b.dlgContent.addView(content)
            b.dlgContent.visibility = View.VISIBLE
        }
        b.dlgCancel.text = cancelText
        b.dlgOk.text = confirmText
        if (danger) {
            b.dlgOk.setBackgroundResource(R.drawable.shape_dialog_btn_danger)
            b.dlgOk.setTextColor(ContextCompat.getColor(context, R.color.white))
        }
        b.dlgCancel.setOnClickListener { dialog.dismiss() }
        b.dlgOk.setOnClickListener { onConfirm(dialog) }

        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.88f).toInt().coerceAtMost((360 * context.resources.displayMetrics.density).toInt()),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
        return dialog
    }

    /** 목록에서 하나를 고르는 창 (신고 사유 등). 처음에는 첫 항목이 골라져 있다. */
    fun choose(
        context: Context,
        title: String,
        labels: List<String>,
        confirmText: String,
        danger: Boolean = false,
        onConfirm: (Int) -> Unit
    ): Dialog {
        var selected = 0
        val dp = context.resources.displayMetrics.density
        val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val rows = ArrayList<TextView>()
        fun paint() {
            rows.forEachIndexed { i, row ->
                val on = i == selected
                row.setTextColor(ContextCompat.getColor(context, if (on) R.color.brand_mint else R.color.white))
                row.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
                row.text = (if (on) "●  " else "○  ") + labels[i]
            }
        }
        labels.forEachIndexed { i, _ ->
            val row = TextView(context).apply {
                textSize = 14f
                gravity = Gravity.CENTER_VERTICAL
                minHeight = (44 * dp).toInt()
                setOnClickListener {
                    selected = i
                    paint()
                }
            }
            rows.add(row)
            list.addView(row)
        }
        paint()
        return custom(context, title, null, list, confirmText, danger = danger) { dialog ->
            dialog.dismiss()
            onConfirm(selected)
        }
    }
}
