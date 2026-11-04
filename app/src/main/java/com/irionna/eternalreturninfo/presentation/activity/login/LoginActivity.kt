package com.irionna.eternalreturninfo.presentation.activity.login

import com.irionna.eternalreturninfo.util.PrivacyConsentDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.FindpwDialogBinding
import com.irionna.eternalreturninfo.databinding.GoogleDialogBinding
import com.irionna.eternalreturninfo.databinding.LoginActivityBinding
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.presentation.activity.main.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginActivityBinding
    private lateinit var binding_: FindpwDialogBinding
    private lateinit var googlebinding: GoogleDialogBinding
    private var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null
    var mGoogleSignInClient: GoogleSignInClient? = null
    var db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        binding = LoginActivityBinding.inflate(layoutInflater)
        binding_ = FindpwDialogBinding.inflate(layoutInflater)
        googlebinding = GoogleDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //구글로그인 기본 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("48907367773-37le51fs370ruk8eirjkmi11k6qmg30k.apps.googleusercontent.com")
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        //로그인 버튼
        binding.loginLoginBtn.setOnClickListener {
            Login(binding.loginIDEt.text.toString(), binding.loginPWEt.text.toString())
        }

        //비밀번호찾기 다이얼로그 버튼
        binding.loginLostBtn.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.findpw_dialog, null)
            val alertDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()
            val Email = dialogView.findViewById<EditText>(R.id.findpw_id_et).text
            val button = dialogView.findViewById<Button>(R.id.findpw_findpw_btn)

            // 비밀번호찾기 이메일 전송 버튼
            button.setOnClickListener {
                if (Email.isNotEmpty()) {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(Email.toString())
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                MotionToast.darkColorToast(
                                    this, "", Email.toString() + getString(R.string.login_lostpw_success),
                                    MotionToastStyle.SUCCESS,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.SHORT_DURATION,
                                    font = null
                                )
                            } else {
                                MotionToast.darkColorToast(
                                    this, "CHECK", getString(R.string.login_lostpw_fail),
                                    MotionToastStyle.WARNING,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.SHORT_DURATION,
                                    font = null
                                )
                            }
                        }
                    alertDialog.dismiss()
                } else {
                    MotionToast.darkColorToast(
                        this, "CHECK", getString(R.string.login_lostpw_null),
                        MotionToastStyle.WARNING,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.SHORT_DURATION,
                        font = null
                    )
                }
            }
            alertDialog.show()
        }

        //회원가입 버튼
        binding.loginSignupBtn.setOnClickListener {
            // 개인정보 수집·이용 안내에 동의해야 가입 화면으로 넘어간다.
            PrivacyConsentDialog.show(this) {
                startActivity(Intent(this, SignUpActivity::class.java))
            }
        }

    }

    // 일반 이메일 로그인
    private fun Login(email: String, pw: String) {
        if (email.isNotEmpty() && pw.isNotEmpty()) {
            auth?.signInWithEmailAndPassword(email, pw)?.addOnCompleteListener {
                if (it.isSuccessful) {
                    // 로그인 성공 후 별도 문서 읽기는 필요 없다. 사용자 정보는 각 화면에서 uid 로 읽는다.
                    BoardSingletone.Login()
                    MotionToast.darkColorToast(
                        this, "", getString(R.string.login_success),
                        MotionToastStyle.SUCCESS,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.SHORT_DURATION,
                        font = null
                    )
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    MotionToast.darkColorToast(
                        this, "", getString(R.string.login_fail),
                        MotionToastStyle.ERROR,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.SHORT_DURATION,
                        font = null
                    )

                }
            }
        } else {
            MotionToast.createColorToast(
                this, "", getString(R.string.login_fail),
                MotionToastStyle.ERROR,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.SHORT_DURATION,
                font = null
            )

        }
    }

    // 구글 로그인
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val google = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(google.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        MotionToast.createColorToast(
                            this, "", task.exception?.message.toString(),
                            MotionToastStyle.ERROR,
                            MotionToast.GRAVITY_BOTTOM,
                            MotionToast.SHORT_DURATION,
                            font = null
                        )

                    }
                }
        }
    }

}
