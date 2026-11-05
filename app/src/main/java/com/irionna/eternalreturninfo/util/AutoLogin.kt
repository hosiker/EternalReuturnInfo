package com.irionna.eternalreturninfo.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * 자동 로그인: 로그인한 뒤 [DURATION_MS](30일) 동안은 앱을 다시 열어도 로그인 화면을 건너뛴다.
 *  - 로그인 때 "자동 로그인"을 체크한 경우에만 시각을 기록한다. (체크하지 않으면 다음 실행 때 로그인 화면)
 *  - 30일은 로그인한 시점부터 센다. 앱을 쓴다고 연장되지 않는다. (연장하려면 [touch] 를 앱 시작 때 부르면 된다)
 *  - 로그아웃·회원탈퇴·만료 시 기록을 지우고 Firebase 세션도 끝낸다.
 * Firebase 인증 자체는 기기에 계속 남아 있으므로, 이 기록이 없거나 기한이 지나면 직접 로그아웃시킨다.
 */
object AutoLogin {
    const val DURATION_MS = 30L * 24 * 60 * 60 * 1000
    private const val PREFS = "auto_login"
    private const val KEY_AT = "logged_in_at"

    /** 로그인 성공 직후 호출. [enabled] 가 false 면 기록을 지운다. */
    fun remember(context: Context, enabled: Boolean) {
        val e = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (enabled) e.putLong(KEY_AT, System.currentTimeMillis()) else e.remove(KEY_AT)
        e.apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_AT).apply()
    }

    /** 로그인 상태이고 기한 안이면 true. 기한이 지났거나 기록이 없는데 로그인 상태면 로그아웃시킨다. */
    fun isActive(context: Context): Boolean {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) return false
        val at = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_AT, 0L)
        val now = System.currentTimeMillis()
        if (at > 0 && now - at in 0 until DURATION_MS) return true
        auth.signOut()
        clear(context)
        return false
    }
}
