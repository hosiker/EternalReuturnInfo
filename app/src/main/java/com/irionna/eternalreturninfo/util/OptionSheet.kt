package com.irionna.eternalreturninfo.util

import android.app.Activity
import android.app.Dialog
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.OptionSheetBinding

/** 아래에서 올라오는 선택 목록. 맨 위 "전체"를 고르면 null 을 돌려준다 (필터 해제). */
object OptionSheet {
    fun show(
        activity: Activity,
        title: String,
        options: List<String>,
        selected: String?,
        allLabel: String,
        onPick: (String?) -> Unit
    ) {
        val b = OptionSheetBinding.inflate(LayoutInflater.from(activity))
        val dialog = Dialog(activity, R.style.ConsentSheetDialog)
        dialog.setContentView(b.root)
        b.optionTitle.text = title
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
        }
        val dp = activity.resources.displayMetrics.density
        fun row(label: String, value: String?) {
            val on = value == selected
            b.optionList.addView(TextView(activity).apply {
                text = label
                textSize = 15f
                minHeight = (48 * dp).toInt()
                gravity = Gravity.CENTER_VERTICAL
                setPadding((4 * dp).toInt(), 0, (4 * dp).toInt(), 0)
                setTextColor(ContextCompat.getColor(activity, if (on) R.color.brand_mint else R.color.white))
                if (on) setTypeface(typeface, Typeface.BOLD)
                setOnClickListener {
                    dialog.dismiss()
                    onPick(value)
                }
            })
        }
        row(allLabel, null)
        options.forEach { row(it, it) }
        dialog.show()
    }
}
