package com.irionna.eternalreturninfo.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.irionna.eternalreturninfo.R

/**
 * 회원가입 전에 개인정보 수집·이용 내용을 요약해서 보여주고, 동의해야 다음 단계로 넘어가게 한다.
 * "방침 보기"는 공개된 개인정보처리방침 페이지를 브라우저로 연다. (팝업은 닫히지 않는다)
 * 동의 여부는 따로 저장하지 않는다. (가입 화면의 "만 14세 이상" 확인과 별개)
 *
 * 팝업의 요약 문구는 docs/privacy-policy.md 와 같은 내용이어야 한다. 방침을 고치면 strings.xml 도 함께 고친다.
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
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { openPolicy(activity) }
    }

    private fun openPolicy(activity: Activity) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PRIVACY_POLICY_URL)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                activity,
                activity.getString(R.string.privacy_policy_open_failed, Constants.PRIVACY_POLICY_URL),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
