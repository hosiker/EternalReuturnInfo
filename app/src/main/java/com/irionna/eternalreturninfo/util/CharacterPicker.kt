package com.irionna.eternalreturninfo.util

import android.app.Activity
import android.app.Dialog
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.CharacterPickerItemBinding
import com.irionna.eternalreturninfo.databinding.CharacterPickerSheetBinding

/**
 * 선호 실험체를 얼굴 그림과 함께 고르는 하단 시트 (4열 그리드 + 이름 검색).
 * 실험체 이미지는 전신 그림이라 얼굴 부분만 미리 잘라 둔 face_{번호} 이미지를 쓴다.
 * names[0] 은 "선택"(선택 안 함)이고, 그림은 ProfileImage 규칙(character_{index})을 따른다.
 */
object CharacterPicker {

    /**
     * 실험체 얼굴 썸네일을 보여준다. 얼굴 그림은 전신 그림에서 미리 잘라 둔 drawable(face_{번호}) 이다.
     * 선택 안 함(0번)은 이미지를 비우고, 얼굴 그림이 없는 실험체는 기본 이미지를 보여준다.
     */
    fun loadFace(imageView: android.widget.ImageView, index: Int) {
        if (index < 1) {
            imageView.setImageDrawable(null)
            return
        }
        val ctx = imageView.context
        val id = ctx.resources.getIdentifier("face_$index", "drawable", ctx.packageName)
        imageView.setImageResource(if (id != 0) id else R.drawable.ic_baseimage)
    }

    fun show(activity: Activity, names: Array<String>, selected: Int, onPicked: (Int) -> Unit) {
        val b = CharacterPickerSheetBinding.inflate(LayoutInflater.from(activity))
        val dialog = Dialog(activity, R.style.ConsentSheetDialog)
        dialog.setContentView(b.root)

        // 화면의 약 75% 높이 시트
        val height = (activity.resources.displayMetrics.heightPixels * 0.75f).toInt()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height)
            setGravity(Gravity.BOTTOM)
            setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        val adapter = Adapter(names, selected) { index ->
            dialog.dismiss()
            onPicked(index)
        }
        b.pickerRv.layoutManager = GridLayoutManager(activity, 4)
        b.pickerRv.adapter = adapter
        b.pickerSearchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = adapter.filter(s?.toString().orEmpty())
        })
        dialog.show()
    }

    private class Adapter(
        private val names: Array<String>,
        private val selected: Int,
        private val onClick: (Int) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {

        private var shown: List<Int> = names.indices.toList()

        fun filter(query: String) {
            val q = query.trim()
            // 검색 중에는 "선택 안 함"(0번)을 숨긴다.
            shown = names.indices.filter { i ->
                if (q.isEmpty()) true else i >= 1 && names[i].contains(q, ignoreCase = true)
            }
            notifyDataSetChanged()
        }

        class VH(val b: CharacterPickerItemBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(CharacterPickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = shown.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val index = shown[position]
            val ctx = holder.b.root.context
            holder.b.pickerName.text = if (index == 0) ctx.getString(R.string.character_picker_none) else names[index]
            loadFace(holder.b.pickerFace, index)
            val isSelected = index == selected
            holder.b.pickerFace.borderWidth = if (isSelected) (3 * ctx.resources.displayMetrics.density).toInt() else 0
            holder.b.pickerName.setTextColor(
                ctx.getColor(if (isSelected) R.color.brand_mint else R.color.white)
            )
            holder.b.root.setOnClickListener { onClick(index) }
        }
    }
}
