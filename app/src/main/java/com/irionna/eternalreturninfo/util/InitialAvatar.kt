package com.irionna.eternalreturninfo.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.irionna.eternalreturninfo.R

/**
 * 얼굴 이미지가 없는 실험체를 위한 대체 아바타: 어두운 원판 위에 이름의 첫 글자. (게임 공식 이미지를 쓰지 않는다)
 * 원형 이미지뷰(CircleImageView)는 크기가 없는 Drawable 을 그리지 못해서(비트맵을 만들다 오류) 실제 비트맵으로 만들어 준다.
 * 같은 글자는 한 번만 만들어 재사용한다.
 */
object InitialAvatar {
    private const val SIZE_PX = 160
    private val cache = HashMap<String, Bitmap>()

    fun drawable(context: Context, name: String): Drawable {
        val letter = name.trim().firstOrNull()?.toString() ?: "?"
        val bitmap = cache.getOrPut(letter) { render(context, letter) }
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun render(context: Context, letter: String): Bitmap {
        val bmp = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(ContextCompat.getColor(context, R.color.surface_stroke))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.brand_mint)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            textSize = SIZE_PX * 0.42f
        }
        val y = SIZE_PX / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(letter, SIZE_PX / 2f, y, paint)
        return bmp
    }
}
