package com.irionna.eternalreturninfo.presentation.activity.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.irionna.eternalreturninfo.BuildConfig
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.repository.AccountDeleter
import com.irionna.eternalreturninfo.data.repository.BlockedUsers
import com.irionna.eternalreturninfo.databinding.SettingsActivityBinding
import com.irionna.eternalreturninfo.presentation.activity.block.BlockedUsersActivity
import com.irionna.eternalreturninfo.presentation.activity.login.LoginActivity
import com.irionna.eternalreturninfo.util.AppDialog
import com.irionna.eternalreturninfo.util.AutoLogin
import com.irionna.eternalreturninfo.util.CharacterPicker
import com.irionna.eternalreturninfo.util.Constants
import com.irionna.eternalreturninfo.util.ProfileImage
import com.irionna.eternalreturninfo.util.setContentViewEdgeToEdge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 프로필 > ⚙ 설정. 계정·안전·정보·탈퇴를 한 화면에 모았다. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: SettingsActivityBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingsActivityBinding.inflate(layoutInflater)
        setContentViewEdgeToEdge(binding.root)

        binding.settingsBackBtn.setOnClickListener { finish() }
        binding.settingsCharacter.setOnClickListener { changeCharacter() }
        binding.settingsPassword.setOnClickListener { showPasswordDialog() }
        binding.settingsLogout.setOnClickListener { logout() }
        binding.settingsBlocked.setOnClickListener {
            startActivity(Intent(this, BlockedUsersActivity::class.java))
        }
        binding.settingsTerms.setOnClickListener { openUrl(Constants.TERMS_URL) }
        binding.settingsPrivacy.setOnClickListener { openUrl(Constants.PRIVACY_POLICY_URL) }
        binding.settingsDeleteGuide.setOnClickListener { openUrl(Constants.ACCOUNT_DELETION_URL) }
        binding.settingsContact.setOnClickListener { sendMail() }
        binding.settingsVersion.text = "${getString(R.string.settings_version)}   ${BuildConfig.VERSION_NAME}"
        binding.settingsWithdraw.setOnClickListener { confirmWithdraw() }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show()
        }
    }

    private fun sendMail() {
        try {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Constants.CONTACT_EMAIL}")))
        } catch (e: Exception) {
            Toast.makeText(this, Constants.CONTACT_EMAIL, Toast.LENGTH_LONG).show()
        }
    }

    private fun changeCharacter() {
        val uid = auth.uid ?: return
        val list = resources.getStringArray(R.array.characterName)
        FirebaseFirestore.getInstance().collection("EternalReturnInfo").document(uid).get()
            .addOnSuccessListener { doc ->
                if (isFinishing) return@addOnSuccessListener
                val current = list.indexOf(doc.getString("character")).coerceAtLeast(0)
                CharacterPicker.show(this, list, current) { picked ->
                    val name = list[picked]
                    val key = ProfileImage.keyForCharacterIndex(picked)
                    FirebaseFirestore.getInstance().collection("EternalReturnInfo").document(uid)
                        .update(mapOf("character" to name, "profile" to key))
                    FirebaseDatabase.getInstance().reference.child("user").child(uid)
                        .updateChildren(mapOf("character" to name, "profilePicture" to key))
                }
            }
    }

    private fun showPasswordDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_input_password, null)
        val first = view.findViewById<EditText>(R.id.dlg_pw1_et)
        val second = view.findViewById<EditText>(R.id.dlg_pw2_et)
        AppDialog.custom(
            this,
            title = getString(R.string.dlg_password_title),
            content = view,
            confirmText = getString(R.string.dlg_password_ok)
        ) { dialog ->
            val pw = first.text.toString()
            val check = second.text.toString()
            when {
                pw.length < 6 -> toast(R.string.pw_too_short)
                pw != check -> toast(R.string.pw_mismatch)
                else -> {
                    val user = auth.currentUser ?: return@custom
                    user.updatePassword(pw)
                        .addOnSuccessListener {
                            toast(R.string.pw_changed)
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Log.w("설정", "비밀번호 변경 실패", e)
                            toast(if (e is FirebaseAuthRecentLoginRequiredException) R.string.pw_need_relogin else R.string.pw_change_failed)
                        }
                }
            }
        }
    }

    private fun toast(res: Int) = Toast.makeText(applicationContext, res, Toast.LENGTH_LONG).show()

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    private fun logout() {
        BlockedUsers.stop()
        auth.signOut()
        AutoLogin.clear(this)
        goToLogin()
    }

    private fun confirmWithdraw() {
        AppDialog.confirm(
            this,
            title = getString(R.string.dlg_withdraw_title),
            message = getString(R.string.dlg_withdraw_message),
            confirmText = getString(R.string.dlg_withdraw_ok),
            danger = true
        ) { withdraw() }
    }

    /** 실제 삭제는 AccountDeleter. 화면이 닫혀도 끝까지 진행되도록 별도 스코프에서 실행한다. */
    private fun withdraw() {
        val appContext = applicationContext
        val host = this
        CoroutineScope(Dispatchers.Main).launch {
            try {
                BlockedUsers.stop()
                AccountDeleter().delete(appContext.getString(R.string.withdrawn_user_name))
                AutoLogin.clear(appContext)
                host.goToLogin()
            } catch (e: AccountDeleter.NeedsRecentLoginException) {
                Toast.makeText(appContext, "보안을 위해 다시 로그인한 뒤 탈퇴해 주세요.", Toast.LENGTH_LONG).show()
            } catch (e: AccountDeleter.StepFailedException) {
                Log.w("계정삭제", "탈퇴 처리 실패: ${e.step}", e)
                Toast.makeText(appContext, "탈퇴 처리에 실패했습니다. (${e.step}) 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.w("계정삭제", "탈퇴 처리 실패", e)
                Toast.makeText(appContext, "탈퇴 처리에 실패했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
