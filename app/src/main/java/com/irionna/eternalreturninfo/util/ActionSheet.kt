package com.irionna.eternalreturninfo.util

import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.OptionSheetBinding

/** 아래에서 올라오는 동작 목록 (수정·삭제·신고·차단 같은 메뉴). 위험한 동작은 붉게 보인다. */
object ActionSheet {
    class Item(val label: String, val danger: Boolean = false, val onClick: () -> Unit)

    fun show(activity: Activity, items: List<Item>) {
        if (items.isEmpty() || activity.isFinishing) return
        val b = OptionSheetBinding.inflate(LayoutInflater.from(activity))
        val dialog = Dialog(activity, R.style.ConsentSheetDialog)
        dialog.setContentView(b.root)
        b.optionTitle.visibility = View.GONE
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
        }
        val dp = activity.resources.displayMetrics.density
        items.forEach { item ->
            b.optionList.addView(TextView(activity).apply {
                text = item.label
                textSize = 15f
                minHeight = (52 * dp).toInt()
                gravity = Gravity.CENTER_VERTICAL
                setPadding((4 * dp).toInt(), 0, (4 * dp).toInt(), 0)
                setTextColor(if (item.danger) 0xFFFF6B6B.toInt() else ContextCompat.getColor(activity, R.color.white))
                setOnClickListener {
                    dialog.dismiss()
                    item.onClick()
                }
            })
        }
        dialog.show()
    }
}
