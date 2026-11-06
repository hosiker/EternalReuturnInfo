package com.irionna.eternalreturninfo.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.irionna.eternalreturninfo.R

/**
 * 얼굴 이미지가 없는 실험체를 위한 대체 아바타: 어두운 원 안에 이름의 첫 글자.
 * (게임 공식 이미지를 쓰지 않는다. 원형 이미지뷰 안에서 그대로 보이게 정사각형 전체를 채운다.)
 */
class InitialAvatar(context: Context, name: String) : Drawable() {
    private val letter = name.trim().firstOrNull()?.toString() ?: "?"
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.surface_stroke) }
    private val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.brand_mint)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        canvas.drawRect(b, bg)
        fg.textSize = b.height() * 0.42f
        val y = b.exactCenterY() - (fg.descent() + fg.ascent()) / 2
        canvas.drawText(letter, b.exactCenterX(), y, fg)
    }

    override fun setAlpha(alpha: Int) { fg.alpha = alpha; bg.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { fg.colorFilter = colorFilter; bg.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.OPAQUE
}
