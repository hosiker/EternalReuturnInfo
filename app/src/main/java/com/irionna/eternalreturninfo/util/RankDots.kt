package com.irionna.eternalreturninfo.util

import android.content.Context
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.irionna.eternalreturninfo.R

/** 최근 경기 순위를 작은 칸으로 나란히 보여준다. 1등(우승)은 초록 별(★), 2~3등과 나머지는 색이 다르다. */
object RankDots {
    /** "1,4,2,7,3" 형식의 저장값을 순위 목록으로 바꾼다. */
    fun parse(raw: String?): List<Int> =
        raw.orEmpty().split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }.take(5)

    fun fill(container: LinearLayout, ranks: List<Int>) {
        container.removeAllViews()
        val ctx = container.context
        val dp = ctx.resources.displayMetrics.density
        ranks.forEachIndexed { i, rank ->
            container.addView(TextView(ctx).apply {
                text = if (rank == 1) "★" else rank.toString()
                textSize = 11f
                gravity = Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                val (bg, fg) = style(ctx, rank)
                setBackgroundResource(bg)
                setTextColor(fg)
                contentDescription = ctx.getString(R.string.rank_desc, rank)
                layoutParams = LinearLayout.LayoutParams((22 * dp).toInt(), (22 * dp).toInt()).apply {
                    if (i > 0) marginStart = (4 * dp).toInt()
                }
            })
        }
    }

    private fun style(ctx: Context, rank: Int): Pair<Int, Int> = when (rank) {
        1 -> R.drawable.shape_rank_1 to ContextCompat.getColor(ctx, R.color.black)
        2 -> R.drawable.shape_rank_2 to ContextCompat.getColor(ctx, R.color.brand_mint)
        3 -> R.drawable.shape_rank_3 to ContextCompat.getColor(ctx, R.color.white)
        else -> R.drawable.shape_rank_n to ContextCompat.getColor(ctx, R.color.text_secondary)
    }
}
