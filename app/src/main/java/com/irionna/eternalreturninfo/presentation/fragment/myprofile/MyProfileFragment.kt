package com.irionna.eternalreturninfo.presentation.fragment.myprofile

import android.content.Intent
import com.google.firebase.database.FirebaseDatabase
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.presentation.activity.settings.SettingsActivity
import com.irionna.eternalreturninfo.util.AutoLogin
import com.irionna.eternalreturninfo.util.CharacterPicker
import com.irionna.eternalreturninfo.data.repository.BlockedUsers
import com.irionna.eternalreturninfo.databinding.MyprofileFragmentBinding
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.presentation.activity.login.LoginActivity
import com.irionna.eternalreturninfo.presentation.activity.login.SignUpActivity
import com.irionna.eternalreturninfo.util.NicknameKey
import com.irionna.eternalreturninfo.util.ProfileImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import com.irionna.eternalreturninfo.presentation.activity.main.MainActivity
import com.irionna.eternalreturninfo.presentation.adapter.myprofile.MyProfileViewPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore

class MyProfileFragment : Fragment() {
    private val binding get() = _binding!!
    private var _binding: MyprofileFragmentBinding? = null
    private var auth: FirebaseAuth? = null
    var db = FirebaseFirestore.getInstance()
    var email: String? = null
    private lateinit var database: DatabaseReference

    companion object {
        fun newInstance() = MyProfileFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = MyprofileFragmentBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        var uid = auth?.uid.toString()
        Patch(uid)
        setOnClickListener()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MyProfileViewPagerAdapter(requireActivity())
        binding.myprofileViewpager2.adapter = adapter

        TabLayoutMediator(
            binding.myprofileTabLayout,
            binding.myprofileViewpager2,
            true,
            false
        ) { tab, position ->
            tab.setText(adapter.getTitle(position))
        }.attach()

        binding.myprofileViewpager2.isUserInputEnabled = false
        binding.myprofileBtnSetting.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }

    private fun setOnClickListener() {
        // 프로필 이미지는 선호 실험체 이미지로 자동 정해지므로 사진을 고르는 동작은 없다.
    }

    // 시트에서 미리 선택해 보여줄 현재 실험체 번호 (characterName 배열 위치, 0 = 선택 안 함)
    private var currentCharacterIndex = 0

    // 마이페이지 생성
    fun Patch(uid: String) {
        val docRef = db.collection("EternalReturnInfo").document("$uid")

        docRef.get()
            .addOnSuccessListener { document ->
                // 응답이 늦게 도착했을 때 화면이 이미 사라졌다면 건드리지 않는다.
                if (_binding == null || !isAdded) return@addOnSuccessListener
                if (document != null) {
                    // 프로필 이미지는 실험체 이미지. 옛 형식(사진 주소)이면 선택한 실험체 기준으로 한 번 보정해 저장한다.
                    var profileKey = document.getString("profile")
                    if (ProfileImage.drawableNameOf(profileKey) == null) {
                        profileKey = ProfileImage.keyForCharacter(
                            document.getString("character"),
                            resources.getStringArray(R.array.characterName)
                        )
                        saveProfileKey(uid, profileKey)
                    }
                    binding.myprofileEmailTv.text = document["email"].toString()
                    binding.myprofileNicknameTv.text = document["nickName"].toString()
                    ProfileImage.load(binding.myprofileProfileImg, profileKey)
                    currentCharacterIndex = resources.getStringArray(R.array.characterName)
                        .indexOf(document.getString("character")).coerceAtLeast(0)
                    email = document["email"].toString()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        var uid = auth?.uid.toString()
        Patch(uid)
    }

    /** 가입자 문서(profile)와 실시간 DB(profilePicture)에 같은 프로필 이미지 값을 저장한다. */
    private fun saveProfileKey(uid: String, profileKey: String) {
        FirebaseFirestore.getInstance()
            .collection("EternalReturnInfo")
            .document(uid)
            .update("profile", profileKey)
        database.child("user").child(uid).updateChildren(mapOf("profilePicture" to profileKey))
    }
}
