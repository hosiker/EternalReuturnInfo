package com.irionna.eternalreturninfo.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.irionna.eternalreturninfo.R

/**
 * 이용약관(커뮤니티 규칙) 동의.
 *  - 신규 가입: PrivacyConsentDialog 에서 개인정보 안내와 함께 동의를 받고, 가입이 끝나면 [markAccepted] 로 기록한다.
 *  - 기존 이용자: 약관 시행 전에 가입한 계정은 앱을 열 때 [showGate] 로 한 번 동의를 받는다.
 * 동의 기록은 서버로 보내지 않고 이 기기에만 계정(uid)별로 저장한다. 약관을 크게 고치면 [VERSION] 을 올려 다시 받는다.
 */
object TermsConsent {
    const val VERSION = 1
    private const val PREFS = "terms_consent"

    private fun key(uid: String) = "accepted_v${VERSION}_$uid"

    fun isAccepted(context: Context, uid: String?): Boolean =
        !uid.isNullOrEmpty() && context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key(uid), false)

    fun markAccepted(context: Context, uid: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key(uid), true).apply()
    }

    /** 앱을 열 때: 아직 동의하지 않은 계정에게 동의 화면을 보여 준다. 동의하지 않으면 [onDeclined] 로 로그아웃시킨다. */
    fun showGate(activity: Activity, onAccepted: () -> Unit, onDeclined: () -> Unit) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.terms_gate_title)
            .setMessage(R.string.terms_gate_message)
            .setCancelable(false)
            .setPositiveButton(R.string.terms_gate_agree) { _, _ -> onAccepted() }
            .setNegativeButton(R.string.terms_gate_decline) { _, _ -> onDeclined() }
            .setNeutralButton(R.string.privacy_consent_view, null)
            .create()
        dialog.show()
        // 문서를 보고 돌아와도 동의 화면이 남아 있도록 리스너를 다시 단다.
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { showDocumentChooser(activity) }
    }

    /** "이용약관 / 개인정보처리방침" 중 열 문서를 고르게 한다 (동의 화면은 그대로 남는다). */
    fun showDocumentChooser(activity: Activity) {
        val items = arrayOf(
            activity.getString(R.string.terms_doc_terms),
            activity.getString(R.string.terms_doc_privacy)
        )
        AlertDialog.Builder(activity)
            .setItems(items) { _, which ->
                openUrl(activity, if (which == 0) Constants.TERMS_URL else Constants.PRIVACY_POLICY_URL)
            }
            .show()
    }

    fun openUrl(activity: Activity, url: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, activity.getString(R.string.privacy_policy_open_failed, url), Toast.LENGTH_LONG).show()
        }
    }
}
