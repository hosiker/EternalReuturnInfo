package com.irionna.eternalreturninfo.presentation.activity.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.SignupInformationActivityBinding
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.data.model.SignUpData
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.retrofit.RetrofitInstance
import com.irionna.eternalreturninfo.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: SignupInformationActivityBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private lateinit var selectedImageURI: Uri
    private val PICK_IMAGE = 1111
    private lateinit var database: DatabaseReference
    val storage = Firebase.storage
    var imageCheck = false
    var emailCheck = false
    var baseImage = "https://firebasestorage.googleapis.com/v0/b/eternalreturninfo-4dc4b.appspot.com/o/ic_baseImage.jpg?alt=media&token=50e58bfe-873f-4772-bddc-a3401dc3d8a3&_gl=1*lgw3h7*_ga*MjY4NTI2NjgxLjE2OTY5MzI3ODU.*_ga_CW55HF8NVT*MTY5OTIzNDQwMS42Ny4xLjE2OTkyMzQ2NjcuOS4wLjA."
    var nickNameCheck = false
    private var signupNickname: String = ""
    private var signupEmail: String = ""

    companion object {
        val collection = "EternalReturnInfo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SignupInformationActivityBinding.inflate(layoutInflater)
        auth = Firebase.auth
        database = Firebase.database.reference
        setContentView(binding.root)

        binding.signupProfileImg.setOnClickListener {
            selectProfile()
        }

        binding.signupBtnIDCheck.setOnClickListener {
            var email = binding.signupIDEt.text.toString()
            Log.d("이메일중복", email)
            if (email.isNotEmpty()) {
                EmailCheck(email)
            } else {
                Toast.makeText(this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        binding.signupBtnNicknameCheck.setOnClickListener {
            NickNameCheck(binding.signupNickNameEt.text.toString())
        }

        // 실험체 스피너
        val characterlist = resources.getStringArray(R.array.characterName)
        val adapter =
            ArrayAdapter<String>(this, R.layout.signup_spinner, R.id.spinner_tv, characterlist)
        var selectCharacter = characterlist[0]
        binding.signupCharacterSp.adapter = adapter

        binding.signupCharacterSp.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectCharacter = characterlist[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

        binding.signupSignupBtn.setOnClickListener {
            createAccount(
                signupEmail,
                binding.signupPWEt.text.toString(),
                binding.signupPWCheckEt.text.toString(),
                signupNickname,
                selectCharacter,
            )
        }
    }

    private fun createAccount(
        email: String,
        password: String,
        passwordCheck: String,
        nickname: String,
        character: String,
    ) {
        if (email.isNotEmpty() && password.isNotEmpty() && passwordCheck.isNotEmpty() && nickname.isNotEmpty()) {
            if (password == passwordCheck) {
                if (!nickNameCheck) {
                    auth?.createUserWithEmailAndPassword(email, password)
                        ?.addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                BoardSingletone.Login()

                                MotionToast.darkColorToast(
                                    this, "", "계정 생성 완료",
                                    MotionToastStyle.SUCCESS,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.SHORT_DURATION,
                                    font = null
                                )
                                setDocument(
                                    SignUpData(
                                        email = email,
                                        nickName = nickname,
                                        character = character,
                                        profile = baseImage
                                    )
                                )
                                database.child("user").child(auth.uid!!)
                                    .setValue(
                                        ERModel(
                                            profilePicture = baseImage,
                                            email = email,
                                            name = nickname,
                                            uid = auth.uid!!
                                        )
                                    )
                                if (imageCheck) {
                                    upload(selectedImageURI, email)
                                }

                                /**
                                 * 파이어베이스 보안규칙 적용 후,
                                 * 바로 데이터를 읽을 수 없는 이슈로 인해
                                 * 회원가입 후 메인화면이 아닌 로그인 화면으로 돌아가는 코드로 수정했습니다.
                                 */
//                                var intent = Intent(this, MainActivity::class.java)
//                                startActivity(intent)
//                                auth.signOut()
                                  finish()
                            } else {
                                MotionToast.darkColorToast(
                                    this, "", "계정 생성 실패",
                                    MotionToastStyle.ERROR,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.SHORT_DURATION,
                                    font = null
                                )
                            }
                        }
                } else {
                    MotionToast.darkColorToast(
                        this, "CHECK", "중복된 닉네임입니다.",
                        MotionToastStyle.WARNING,
                        MotionToast.GRAVITY_BOTTOM,
                        MotionToast.SHORT_DURATION,
                        font = null
                    )
                }
            } else {
                MotionToast.darkColorToast(
                    this, "CHECK", "같은 비밀번호를 입력하세요.",
                    MotionToastStyle.WARNING,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.SHORT_DURATION,
                    font = null
                )
            }
        } else {
            MotionToast.darkColorToast(
                this, "CHECK", "회원가입 정보를 입력하세요.",
                MotionToastStyle.WARNING,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.SHORT_DURATION,
                font = null
            )
        }
    }

    private fun setDocument(data: SignUpData) {
        FirebaseFirestore.getInstance()
            .collection("EternalReturnInfo")
            .document(auth.uid!!)
            .set(data)
            .addOnSuccessListener {
            }
            .addOnFailureListener {
                MotionToast.darkColorToast(
                    this, "ERROR", "닉네임 값 저장 실패",
                    MotionToastStyle.ERROR,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.SHORT_DURATION,
                    font = null
                )
            }
    }

    fun selectProfile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(intent, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE) {
            val uri: Uri? = data?.data
            if (uri != null) {
                selectedImageURI = uri
                binding.signupProfileImg.setImageURI(uri)
                imageCheck = true
            }
        }
    }

    fun upload(
        uri: Uri,
        email: String,
    ) {
        val storageRef = storage.reference
        val fileName = email + "_profile"
        val riversRef = storageRef.child("/$fileName")

        riversRef.putFile(uri)
            .addOnProgressListener { taskSnapshot ->
                riversRef.downloadUrl.addOnSuccessListener { uri ->
                    auth.uid?.let {
                        db.collection(collection)
                            .document(it)
                            .update("profile", uri.toString())
                    }
                    database.child("user").child(auth.uid!!).updateChildren(
                        mapOf(
                            "profilePicture" to uri.toString()
                        )
                    )
                }
            }
            .addOnFailureListener { Log.i("업로드 실패", "") }
            .addOnSuccessListener { Log.i("업로드 성공", "") }
    }

    fun EmailCheck(email: String) {
        emailCheck = false
        db.collection(collection)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d("이메일중복확인",document.data["email"].toString())
                    if (email == document.data["email"].toString()) {
                        emailCheck = true
                        Log.d("이메일체크",emailCheck.toString())
                    }
                }
                GlobalScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        if (emailCheck) {
                            binding.signupTvIDcheckMessage.visibility = View.VISIBLE
                            binding.signupTvIDcheckMessage.setTextColor(
                                ContextCompat.getColor(
                                    this@SignUpActivity,
                                    R.color.highlight_color2
                                )
                            )
                            binding.signupTvIDcheckMessage.text = "중복된 이메일이 존재합니다."
                            signupEmail = ""
                        } else {
                            binding.signupTvIDcheckMessage.visibility = View.VISIBLE
                            binding.signupTvIDcheckMessage.setTextColor(
                                ContextCompat.getColor(
                                    this@SignUpActivity,
                                    R.color.highlight_color
                                )
                            )
                            binding.signupTvIDcheckMessage.text = "사용 가능한 이메일입니다."
                            signupEmail = email
                        }

                    }
                }
            }
    }

    fun NickNameCheck(nickname: String) {
        nickNameCheck = false
        db.collection(collection)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (nickname == document.data["nickName"]) {
                        nickNameCheck = true
                    }
                }
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val nickname = binding.signupNickNameEt.text.toString()
                        //수정 : 로그인한 사람 닉네임 가져오기
                        val userID_call = RetrofitInstance.searchUserIDApi.getUserByNickname(Constants.MAIN_APIKEY, nickname)
                        val userID_response = userID_call.execute()

                        if (userID_response.isSuccessful) {
                            val gameResponse = userID_response.body()

                            withContext(Dispatchers.Main) {
                                if (gameResponse?.user == null) {
                                    binding.signupTvCheckMessage.visibility = View.VISIBLE
                                    binding.signupTvCheckMessage.setTextColor(
                                        ContextCompat.getColor(
                                            this@SignUpActivity,
                                            R.color.yellow
                                        )
                                    )
                                    binding.signupTvCheckMessage.text = "닉네임이 존재하지 않습니다 \n일부 기능이 제한됩니다."
                                    signupNickname = nickname
                                    nickNameCheck = false
                                } else if (nickNameCheck) {
                                    binding.signupTvCheckMessage.visibility = View.VISIBLE
                                    binding.signupTvCheckMessage.setTextColor(
                                        ContextCompat.getColor(
                                            this@SignUpActivity,
                                            R.color.highlight_color2
                                        )
                                    )
                                    binding.signupTvCheckMessage.text = "중복된 닉네임입니다."
                                    signupNickname = ""
                                } else {
                                    binding.signupTvCheckMessage.visibility = View.VISIBLE
                                    binding.signupTvCheckMessage.setTextColor(
                                        ContextCompat.getColor(
                                            this@SignUpActivity,
                                            R.color.highlight_color
                                        )
                                    )
                                    binding.signupTvCheckMessage.text = "사용 가능한 닉네임입니다."
                                    signupNickname = gameResponse.user.nickname
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // 오류 처리
                        e.printStackTrace()
                    }
                }
            }
            .addOnFailureListener { exception ->
            }
    }

}

