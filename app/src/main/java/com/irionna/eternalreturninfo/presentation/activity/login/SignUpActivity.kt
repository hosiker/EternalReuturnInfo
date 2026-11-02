package com.irionna.eternalreturninfo.presentation.activity.login

import android.os.Bundle
import android.util.Log
import android.util.Patterns
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
import com.irionna.eternalreturninfo.util.NicknameKey
import com.irionna.eternalreturninfo.util.ProfileImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
    private lateinit var database: DatabaseReference
    var nickNameCheck = false
    private var signupNickname: String = ""
    private var signupEmail: String = ""

    companion object {
        val collection = "EternalReturnInfo"
        const val NICKNAME_COLLECTION = "nicknames"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SignupInformationActivityBinding.inflate(layoutInflater)
        auth = Firebase.auth
        database = Firebase.database.reference
        setContentView(binding.root)

        binding.signupBtnIDCheck.setOnClickListener {
            val email = binding.signupIDEt.text.toString().trim()
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
        // 프로필 이미지는 선택한 실험체 이미지로 자동 정해진다.
        ProfileImage.load(binding.signupProfileImg, ProfileImage.keyForCharacter(selectCharacter, characterlist))

        binding.signupCharacterSp.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectCharacter = characterlist[position]
                    ProfileImage.load(
                        binding.signupProfileImg,
                        ProfileImage.keyForCharacter(selectCharacter, characterlist)
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

        // 만 14세 이상 확인에 체크하기 전에는 가입 버튼을 흐리게 보여준다.
        // 버튼은 눌리게 두어서, 체크하지 않고 누르면 이유를 안내한다.
        binding.signupSignupBtn.alpha = if (binding.signupAgeCheck.isChecked) 1f else 0.4f
        binding.signupAgeCheck.setOnCheckedChangeListener { _, checked ->
            binding.signupSignupBtn.alpha = if (checked) 1f else 0.4f
        }

        binding.signupSignupBtn.setOnClickListener {
            createAccount(
                binding.signupIDEt.text.toString().trim(),
                binding.signupPWEt.text.toString(),
                binding.signupPWCheckEt.text.toString(),
                signupNickname,
                selectCharacter,
            )
        }
    }

    private fun toast(title: String, message: String, style: MotionToastStyle) {
        MotionToast.darkColorToast(
            this, title, message, style,
            MotionToast.GRAVITY_BOTTOM,
            MotionToast.SHORT_DURATION,
            font = null
        )
    }

    private fun createAccount(
        email: String,
        password: String,
        passwordCheck: String,
        nickname: String,
        character: String,
    ) {
        if (email.isEmpty()) {
            toast("CHECK", "이메일을 입력하세요.", MotionToastStyle.WARNING)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("CHECK", "이메일 형식이 올바르지 않습니다.", MotionToastStyle.WARNING)
            return
        }
        if (password.isEmpty() || passwordCheck.isEmpty()) {
            toast("CHECK", "비밀번호를 입력하세요. (6자 이상)", MotionToastStyle.WARNING)
            return
        }
        if (password != passwordCheck) {
            toast("CHECK", "같은 비밀번호를 입력하세요.", MotionToastStyle.WARNING)
            return
        }
        val typedNickname = binding.signupNickNameEt.text.toString()
        if (typedNickname.isBlank()) {
            toast("CHECK", "닉네임을 입력하세요.", MotionToastStyle.WARNING)
            return
        }
        val nicknameKey = NicknameKey.normalize(nickname)
        if (nicknameKey == null || nicknameKey != NicknameKey.normalize(typedNickname)) {
            toast("CHECK", "닉네임 [중복확인]을 눌러주세요.", MotionToastStyle.WARNING)
            return
        }
        if (nickNameCheck) {
            toast("CHECK", "중복된 닉네임입니다.", MotionToastStyle.WARNING)
            return
        }

        if (!binding.signupAgeCheck.isChecked) {
            toast("CHECK", "만 14세 이상 확인에 체크해주세요.", MotionToastStyle.WARNING)
            return
        }

        binding.signupSignupBtn.isEnabled = false
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    binding.signupSignupBtn.isEnabled = true
                    val message = when (task.exception) {
                        is FirebaseAuthUserCollisionException -> "이미 사용 중인 이메일입니다."
                        is FirebaseAuthWeakPasswordException -> "비밀번호는 6자 이상이어야 합니다."
                        is FirebaseAuthInvalidCredentialsException -> "이메일 형식이 올바르지 않습니다."
                        else -> "계정 생성 실패"
                    }
                    toast("", message, MotionToastStyle.ERROR)
                    return@addOnCompleteListener
                }

                val uid = auth.uid!!
                val profileKey = ProfileImage.keyForCharacter(
                    character, resources.getStringArray(R.array.characterName)
                )
                val data = SignUpData(
                    email = email,
                    nickName = nickname,
                    character = character,
                    profile = profileKey
                )
                saveAccountData(uid, data, nicknameKey) { saved ->
                    if (!saved) {
                        // 닉네임을 먼저 선점당했거나 저장에 실패한 경우: 만든 계정을 되돌린다.
                        auth.currentUser?.delete()
                        binding.signupSignupBtn.isEnabled = true
                        toast("", "닉네임이 이미 사용 중이거나 저장에 실패했습니다. 다시 시도해주세요.", MotionToastStyle.ERROR)
                        return@saveAccountData
                    }
                    BoardSingletone.Login()
                    toast("", "계정 생성 완료", MotionToastStyle.SUCCESS)
                    // 이 노드는 로그인한 모든 사용자가 읽을 수 있으므로 이메일 같은 개인정보는 저장하지 않는다.
                    database.child("user").child(uid)
                        .setValue(
                            ERModel(
                                profilePicture = profileKey,
                                name = nickname,
                                uid = uid
                            )
                        )
                    // 가입 후에는 메인이 아니라 로그인 화면으로 돌아간다.
                    finish()
                }
            }
    }

    /**
     * 닉네임 선점 문서(nicknames/{키})와 가입자 문서(EternalReturnInfo/{uid})를 한 번에 저장한다.
     * 규칙이 "이미 있는 nicknames 문서는 create 불가"라서, 같은 닉네임을 동시에 가입하면 한쪽은 실패한다.
     */
    private fun saveAccountData(
        uid: String,
        data: SignUpData,
        nicknameKey: String,
        onResult: (Boolean) -> Unit
    ) {
        val batch = db.batch()
        batch.set(db.collection(NICKNAME_COLLECTION).document(nicknameKey), mapOf("uid" to uid))
        batch.set(db.collection(collection).document(uid), data)
        batch.commit().addOnCompleteListener { onResult(it.isSuccessful) }
    }

    /**
     * 이메일은 형식만 확인한다. 이미 가입된 이메일인지는 가입 버튼을 눌렀을 때 Auth 가 알려준다.
     * (가입자 컬렉션 전체를 읽는 방식은 보안 규칙으로 막혀 있다.)
     */
    fun EmailCheck(email: String) {
        signupEmail = ""
        binding.signupTvIDcheckMessage.visibility = View.VISIBLE
        if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.signupTvIDcheckMessage.setTextColor(
                ContextCompat.getColor(this, R.color.highlight_color)
            )
            binding.signupTvIDcheckMessage.text = "사용할 수 있는 형식입니다. (중복 여부는 가입할 때 확인됩니다)"
            signupEmail = email
        } else {
            binding.signupTvIDcheckMessage.setTextColor(
                ContextCompat.getColor(this, R.color.highlight_color2)
            )
            binding.signupTvIDcheckMessage.text = "이메일 형식이 올바르지 않습니다."
        }
    }

    private fun showNicknameMessage(message: String, colorRes: Int) {
        binding.signupTvCheckMessage.visibility = View.VISIBLE
        binding.signupTvCheckMessage.setTextColor(ContextCompat.getColor(this, colorRes))
        binding.signupTvCheckMessage.text = message
    }

    /** nicknames/{키} 문서 한 건만 읽어서 중복을 확인하고, 이어서 게임 닉네임을 확인한다. */
    fun NickNameCheck(nickname: String) {
        nickNameCheck = false
        signupNickname = ""
        val typed = nickname.trim()
        val key = NicknameKey.normalize(typed)
        if (key == null) {
            showNicknameMessage("사용할 수 없는 닉네임입니다. (빈 값, '/' 포함, 너무 긴 값)", R.color.highlight_color2)
            return
        }
        db.collection(NICKNAME_COLLECTION).document(key)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    nickNameCheck = true
                    showNicknameMessage("중복된 닉네임입니다.", R.color.highlight_color2)
                } else {
                    checkGameNickname(typed, key)
                }
            }
            .addOnFailureListener {
                showNicknameMessage("닉네임 확인에 실패했습니다. 잠시 후 다시 시도해주세요.", R.color.highlight_color2)
            }
    }

    /**
     * 게임(이터널 리턴)에 있는 닉네임인지 확인한다. 가입 가능 여부와는 별개다.
     * 확인할 수 없거나 없는 닉네임이어도 가입은 허용하고, 일부 기능이 제한된다고만 알려준다.
     */
    private fun checkGameNickname(typed: String, key: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val gameNickname: String? = try {
                val response = RetrofitInstance.searchUserIDApi
                    .getUserByNickname(Constants.MAIN_APIKEY, typed).execute()
                if (response.isSuccessful) response.body()?.user?.nickname else null
            } catch (e: Exception) {
                Log.w("닉네임확인", "게임 닉네임 조회 실패", e)
                null
            }
            withContext(Dispatchers.Main) {
                if (gameNickname != null) {
                    signupNickname = if (NicknameKey.normalize(gameNickname) == key) gameNickname else typed
                    showNicknameMessage("사용 가능한 닉네임입니다.", R.color.highlight_color)
                } else {
                    signupNickname = typed
                    showNicknameMessage("게임 닉네임을 확인할 수 없습니다.\n일부 기능이 제한됩니다.", R.color.yellow)
                }
            }
        }
    }

}
