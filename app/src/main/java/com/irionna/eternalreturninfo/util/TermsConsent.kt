package com.irionna.eternalreturninfo.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.app.Dialog
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.ConsentChecksBinding
import com.irionna.eternalreturninfo.databinding.TermsSheetBinding

/**
 * 이용약관(커뮤니티 규칙) 동의.
 *  - 신규 가입: 회원가입 화면 아래의 동의 항목(consent_checks)에서 받고, 가입이 끝나면 [markAccepted] 로 기록한다.
 *  - 기존 이용자: 약관 시행 전에 가입한 계정은 앱을 열 때 [showGate] 의 동의 시트로 한 번 동의를 받는다.
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

    /** 앱을 열 때: 아직 동의하지 않은 계정에게 동의 시트를 보여 준다. 동의하지 않으면 [onDeclined] 로 로그아웃시킨다. */
    fun showGate(activity: Activity, onAccepted: () -> Unit, onDeclined: () -> Unit) {
        val sheet = TermsSheetBinding.inflate(LayoutInflater.from(activity))
        val dialog = Dialog(activity, R.style.ConsentSheetDialog)
        dialog.setContentView(sheet.root)
        dialog.setCancelable(false)

        sheet.sheetAgreeBtn.isEnabled = false
        sheet.sheetAgreeBtn.alpha = 0.4f
        bindConsentChecks(activity, sheet.sheetConsent) { all ->
            sheet.sheetAgreeBtn.isEnabled = all
            sheet.sheetAgreeBtn.alpha = if (all) 1f else 0.4f
        }
        sheet.sheetAgreeBtn.setOnClickListener {
            dialog.dismiss()
            onAccepted()
        }
        sheet.sheetDeclineBtn.setOnClickListener {
            dialog.dismiss()
            onDeclined()
        }
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
        }
        dialog.show()
    }

    /**
     * 동의 항목(모두 동의 + 필수 3개)을 연결한다. 필수 항목이 모두 체크되면 [onChanged] 에 true 를 알린다.
     * "보기"는 해당 문서를 브라우저로 연다 (동의 화면은 그대로 남는다).
     */
    fun bindConsentChecks(activity: Activity, b: ConsentChecksBinding, onChanged: (Boolean) -> Unit) {
        val items = listOf(b.consentTerms, b.consentPrivacy, b.consentAge)
        var updating = false
        fun refresh() {
            val all = items.all { it.isChecked }
            updating = true
            b.consentAll.isChecked = all
            updating = false
            onChanged(all)
        }
        items.forEach { it.setOnCheckedChangeListener { _, _ -> if (!updating) refresh() } }
        b.consentAll.setOnCheckedChangeListener { _, checked ->
            if (updating) return@setOnCheckedChangeListener
            updating = true
            items.forEach { it.isChecked = checked }
            updating = false
            onChanged(checked)
        }
        b.consentTermsView.setOnClickListener { openUrl(activity, Constants.TERMS_URL) }
        b.consentPrivacyView.setOnClickListener { openUrl(activity, Constants.PRIVACY_POLICY_URL) }
        onChanged(false)
    }

    fun openUrl(activity: Activity, url: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, activity.getString(R.string.privacy_policy_open_failed, url), Toast.LENGTH_LONG).show()
        }
    }
}
