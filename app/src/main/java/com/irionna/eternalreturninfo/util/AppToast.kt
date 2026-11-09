package com.irionna.eternalreturninfo.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.irionna.eternalreturninfo.databinding.AppToastBinding

/**
 * 앱 전체에서 같이 쓰는 알림 (아래에서 올라오는 알약 카드). 성공·주의·오류는 아이콘 색으로 구분한다.
 *  - 화면(Activity) 위에 올려서 보여 주고, 한 번에 하나만 보인다.
 *  - 화면이 없거나 곧 닫히는 화면이면 기본 토스트로 대신한다. 창(팝업) 위에 띄워야 하면 [overDialog] 를 켠다.
 *  - 로그인 직후처럼 화면이 바뀌는 알림은 [later] 로 맡기면 다음 화면에서 보여 준다.
 */
object AppToast {
    enum class Kind { SUCCESS, WARNING, ERROR, INFO }

    private const val TAG_VIEW = "app_toast_view"
    private const val SHOW_MS = 2600L
    private val handler = Handler(Looper.getMainLooper())

    private var pendingText: String? = null
    private var pendingKind: Kind = Kind.INFO

    fun show(context: Context, @StringRes res: Int, kind: Kind = Kind.INFO, overDialog: Boolean = false) =
        show(context, context.getString(res), kind, overDialog)

    fun show(context: Context, message: String, kind: Kind = Kind.INFO, overDialog: Boolean = false) {
        val activity = unwrap(context)
        if (overDialog || activity == null || activity.isFinishing || activity.isDestroyed) {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            return
        }
        val host = activity.findViewById<ViewGroup>(android.R.id.content) ?: run {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
            return
        }
        // 이미 떠 있는 알림은 치우고 새로 보인다
        host.findViewWithTag<View>(TAG_VIEW)?.let { host.removeView(it) }

        val b = AppToastBinding.inflate(LayoutInflater.from(activity), host, false)
        b.root.tag = TAG_VIEW
        b.toastText.text = message
        val (symbol, color) = when (kind) {
            Kind.SUCCESS -> "✓" to 0xFF2DB67E.toInt()
            Kind.WARNING -> "!" to 0xFFE8B341.toInt()
            Kind.ERROR -> "✕" to 0xFFE5484D.toInt()
            Kind.INFO -> "i" to 0xFF9AA0AE.toInt()
        }
        b.toastIcon.text = symbol
        (b.toastIcon.background.mutate() as GradientDrawable).setColor(color)
        if (kind == Kind.ERROR) b.toastIcon.setTextColor(0xFFFFFFFF.toInt())

        val dp = activity.resources.displayMetrics.density
        val bars = ViewCompat.getRootWindowInsets(host)?.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
        )
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = android.view.Gravity.BOTTOM
            val side = (18 * dp).toInt()
            setMargins(side, 0, side, (24 * dp).toInt() + (bars?.bottom ?: 0))
        }
        b.root.layoutParams = lp
        b.root.alpha = 0f
        b.root.translationY = 24 * dp
        host.addView(b.root)
        b.root.animate().alpha(1f).translationY(0f).setDuration(180).start()

        val view = b.root
        handler.postDelayed({
            view.animate().alpha(0f).translationY(24 * dp).setDuration(180).withEndAction {
                (view.parent as? ViewGroup)?.removeView(view)
            }.start()
        }, SHOW_MS)
    }

    /** 화면이 바뀌고 나서 보여 줄 알림을 맡겨 둔다. 다음 화면이 [showPending] 을 부른다. */
    fun later(message: String, kind: Kind = Kind.INFO) {
        pendingText = message
        pendingKind = kind
    }

    fun showPending(activity: Activity) {
        val text = pendingText ?: return
        pendingText = null
        show(activity, text, pendingKind)
    }

    private fun unwrap(context: Context): Activity? {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is Activity) return c
            c = c.baseContext
        }
        return null
    }
}
