package com.irionna.eternalreturninfo.presentation.activity.login

import com.irionna.eternalreturninfo.util.setContentViewEdgeToEdge
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.util.AppDialog
import com.irionna.eternalreturninfo.util.AppToast
import com.irionna.eternalreturninfo.util.AutoLogin
import com.irionna.eternalreturninfo.databinding.LoginActivityBinding
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.presentation.activity.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        window.decorView.post { AppToast.showPending(this) }
    }
    private lateinit var binding: LoginActivityBinding
    private var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null
    var db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentViewEdgeToEdge(binding.root)


        //로그인 버튼
        binding.loginLoginBtn.setOnClickListener {
            Login(binding.loginIDEt.text.toString(), binding.loginPWEt.text.toString())
        }

        // 키보드의 완료 버튼으로도 로그인
        binding.loginPWEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                binding.loginLoginBtn.performClick()
                true
            } else false
        }

        //비밀번호찾기 다이얼로그 버튼
        binding.loginLostBtn.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_input_email, null)
            val emailEt = dialogView as EditText
            AppDialog.custom(
                this,
                title = getString(R.string.dlg_findpw_title),
                message = getString(R.string.dlg_findpw_message),
                content = dialogView,
                confirmText = getString(R.string.dlg_findpw_ok)
            ) { dialog ->
                val email = emailEt.text.toString().trim()
                if (email.isEmpty()) {
                    AppToast.show(this, getString(R.string.login_lostpw_null), AppToast.Kind.WARNING)
                } else {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                AppToast.show(this, email + getString(R.string.login_lostpw_success), AppToast.Kind.SUCCESS)
                            } else {
                                AppToast.show(this, getString(R.string.login_lostpw_fail), AppToast.Kind.WARNING)
                            }
                        }
                    dialog.dismiss()
                }
            }
        }

        //회원가입 버튼
        binding.loginSignupBtn.setOnClickListener {
            // 약관·개인정보 동의는 가입 화면 아래쪽에서 받는다.
            startActivity(Intent(this, SignUpActivity::class.java))
        }

    }

    // 일반 이메일 로그인
    private fun Login(email: String, pw: String) {
        if (email.isNotEmpty() && pw.isNotEmpty()) {
            auth?.signInWithEmailAndPassword(email, pw)?.addOnCompleteListener {
                if (it.isSuccessful) {
                    // 로그인 성공 후 별도 문서 읽기는 필요 없다. 사용자 정보는 각 화면에서 uid 로 읽는다.
                    BoardSingletone.Login()
                    // "자동 로그인"을 체크했으면 30일 동안 로그인 화면을 건너뛴다
                    AutoLogin.remember(this, binding.loginAutoCb.isChecked)
                    AppToast.later(getString(R.string.login_success), AppToast.Kind.SUCCESS)
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    AppToast.show(this, getString(R.string.login_fail), AppToast.Kind.ERROR)

                }
            }
        } else {
            AppToast.show(this, getString(R.string.login_fail), AppToast.Kind.ERROR)

        }
    }

}
