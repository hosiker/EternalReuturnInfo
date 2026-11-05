package com.irionna.eternalreturninfo.util

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

/**
 * 항목이 많은 목록(시즌 선택 등)에서 중간 항목을 고르기 쉽게, 한 번의 손짓·마우스 휠로 너무 멀리 넘어가지 않게 하는 스크롤뷰.
 *  - 튕겨 넘기기(fling) 속도를 줄인다.
 *  - 마우스 휠 한 칸은 고정된 작은 거리만 움직인다. (에뮬레이터의 휠은 기본값이 매우 크다)
 */
class SlowScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    override fun fling(velocityY: Int) {
        super.fling((velocityY * FLING_FACTOR).toInt())
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL) {
            val v = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (v != 0f) {
                scrollBy(0, (-v * WHEEL_STEP_DP * resources.displayMetrics.density).toInt())
                return true
            }
        }
        return super.onGenericMotionEvent(event)
    }

    private companion object {
        const val FLING_FACTOR = 0.35f
        const val WHEEL_STEP_DP = 40f
    }
}
