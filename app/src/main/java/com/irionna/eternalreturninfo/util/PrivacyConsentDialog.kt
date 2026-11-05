package com.irionna.eternalreturninfo.util

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.irionna.eternalreturninfo.R

/**
 * 회원가입 전에 개인정보 수집·이용 내용과 이용약관(커뮤니티 규칙) 요약을 보여주고, 동의해야 다음 단계로 넘어가게 한다.
 * "약관·방침 보기"는 이용약관과 개인정보처리방침 중 열 문서를 고르게 하고 브라우저로 연다. (팝업은 닫히지 않는다)
 * 가입이 끝나면 SignUpActivity 가 TermsConsent.markAccepted 로 이 기기에 약관 동의를 기록한다.
 *
 * 팝업의 요약 문구는 docs/privacy-policy.md 와 docs/terms.md 와 같은 내용이어야 한다. 문서를 고치면 strings.xml 도 함께 고친다.
 */
object PrivacyConsentDialog {
    fun show(activity: Activity, onAgree: () -> Unit) {
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.privacy_consent_title)
            .setMessage(R.string.privacy_consent_message)
            .setPositiveButton(R.string.privacy_consent_agree) { _, _ -> onAgree() }
            .setNegativeButton(R.string.privacy_consent_cancel, null)
            .setNeutralButton(R.string.privacy_consent_view, null)
            .create()
        dialog.show()
        // 기본 동작은 버튼을 누르면 팝업이 닫히므로, 방침을 보고 돌아와도 팝업이 남아 있게 리스너를 다시 단다.
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { TermsConsent.showDocumentChooser(activity) }
    }
}
