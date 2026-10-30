package com.irionna.eternalreturninfo.presentation.fragment.myprofile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.MyprofileCharacterDialogBinding
import com.irionna.eternalreturninfo.databinding.MyprofileFragmentBinding
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.presentation.activity.login.LoginActivity
import com.irionna.eternalreturninfo.presentation.activity.login.SignUpActivity
import com.irionna.eternalreturninfo.presentation.activity.main.MainActivity
import com.irionna.eternalreturninfo.presentation.adapter.myprofile.MyProfileViewPagerAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.skydoves.powermenu.CircularEffect
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.OnMenuItemClickListener
import com.skydoves.powermenu.PowerMenu
import com.skydoves.powermenu.PowerMenuItem
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

class MyProfileFragment : Fragment() {
    private val binding get() = _binding!!
    private var _binding: MyprofileFragmentBinding? = null
    private var auth: FirebaseAuth? = null
    var db = Firebase.firestore
    private lateinit var characterbinding: MyprofileCharacterDialogBinding
    private val PICK_IMAGE = 1111
    val storage = Firebase.storage
    var email: String? = null
    private lateinit var database: DatabaseReference

    companion object {
        fun newInstance() = MyProfileFragment()
    }

    private val refPowerMenu: PowerMenu by lazy {
        PowerMenu.Builder(requireContext())
            .addItem(PowerMenuItem("프로필 수정"))
            .addItem(PowerMenuItem("로그아웃"))
            .addItem(PowerMenuItem("회원 탈퇴"))
            .addItem(PowerMenuItem("비밀번호 변경"))
            .setMenuRadius(20f) // sets the corner radius.
            .setTextSize(18)
            .setWidth(500)
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            .setMenuColor(ContextCompat.getColor(requireContext(), R.color.darkgray))
            .setSelectedMenuColor(ContextCompat.getColor(requireContext(), R.color.black))
            .setOnMenuItemClickListener(onMenuItemClickListener)
            .setLifecycleOwner(viewLifecycleOwner)
            .setCircularEffect(CircularEffect.BODY)
            .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = MyprofileFragmentBinding.inflate(inflater, container, false)
        characterbinding = MyprofileCharacterDialogBinding.inflate(layoutInflater)
        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference
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
            binding.myprofileViewpager2
        ) { tab, position ->
            tab.setText(adapter.getTitle(position))
        }.attach()

        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val maxScroll = appBarLayout.totalScrollRange
            val percentage = Math.abs(verticalOffset).toFloat() / maxScroll.toFloat()

            if (percentage == 1.0f) {
                binding.myprofileTvUsername.visibility = View.VISIBLE
                binding.myprofileBtnSetting.visibility = View.INVISIBLE
                binding.myprofileTvUsername.text = BoardSingletone.LoginUser().name
            } else if (percentage == 0.0f) {
                binding.myprofileBtnSetting.visibility = View.VISIBLE
                binding.myprofileTvUsername.visibility = View.INVISIBLE
            } else {
                binding.myprofileBtnSetting.visibility = View.INVISIBLE
                binding.myprofileTvUsername.visibility = View.INVISIBLE
            }

        })

        binding.myprofileBtnSetting.setOnClickListener {
            refPowerMenu.showAsDropDown(it)
        }
    }

    private fun setOnClickListener() {
        val profileBtn = binding.myprofileProfileImg
        profileBtn.setOnClickListener {
            selectProfile()
        }
    }

    // 마이페이지 생성
    fun Patch(uid: String) {
        val docRef = db.collection("EternalReturnInfo").document("$uid")

        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    var uri = Uri.parse(document["profile"].toString())
                    binding.myprofileEmailTv.text = document["email"].toString()
                    binding.myprofileNicknameTv.text = document["nickName"].toString()
                    Glide.with(this).load(uri).into(binding.myprofileProfileImg);
                    email = document["email"].toString()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        var uid = auth?.uid.toString()
        Patch(uid)
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
                upload(uri, email!!)
            }
        }
    }

    fun upload(
        uri: Uri,
        email: String,
    ) {
        val storageRef = storage.reference
        val fileName = email + ".jpg"
        val riversRef = storageRef.child("/$fileName")

        riversRef.putFile(uri)
            .addOnProgressListener { taskSnapshot ->
                riversRef.downloadUrl.addOnSuccessListener { uri ->
                    FirebaseFirestore.getInstance()
                        .collection("EternalReturnInfo")
                        .document(auth!!.uid!!)
                        .update("profile", uri.toString())
                    database.child("user").child(auth!!.uid!!).updateChildren(
                        mapOf(
                            "profilePicture" to uri.toString()
                        )
                    )
                    var uid = auth?.uid
                    uid?.let { Patch(it) }
                }
            }
            .addOnFailureListener { Log.i("업로드 실패", "") }
            .addOnSuccessListener { Log.i("업로드 성공", "") }
    }

    fun PwChange(pw: String, pwCheck: String) {
        val user: FirebaseUser? = auth?.currentUser
        user?.updatePassword(pw)
    }

    // 팝업메뉴 onClick 리스너
    private val onMenuItemClickListener = object : OnMenuItemClickListener<PowerMenuItem> {
        override fun onItemClick(position: Int, item: PowerMenuItem) {
            when (position) {
                // 0 : 프로필수정,   1 : 로그아웃,   2 : 회원탈퇴, 3 : 비밀번호 변경
                0 -> {
                    refPowerMenu.dismiss()
                    val dialogView =
                        layoutInflater.inflate(R.layout.myprofile_character_dialog, null)
                    val alertDialog = AlertDialog.Builder(requireActivity())
                        .setView(dialogView)
                        .create()

                    val characterSpinner =
                        dialogView.findViewById<Spinner>(R.id.myprofile_character_sp)
                    val button = dialogView.findViewById<Button>(R.id.myprofile_select_btn)
                    val deleteBtn = dialogView.findViewById<Button>(R.id.myprofile_delete_btn)
                    val characterlist = resources.getStringArray(R.array.character)

                    val adapter = ArrayAdapter<String>(
                        requireContext(),
                        R.layout.signup_spinner,
                        R.id.spinner_tv,
                        characterlist
                    )

                    var selectCharacter = characterlist[0]
                    characterSpinner.adapter = adapter

                    characterSpinner.onItemSelectedListener =
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
                    // 프로필 변경버튼
                    button.setOnClickListener {
                        FirebaseFirestore.getInstance()
                            .collection("EternalReturnInfo")
                            .document(auth!!.uid!!)
                            .update(
                                mapOf(
                                    "character" to selectCharacter
                                )
                            )
                        database.child("user").child(auth!!.uid!!).updateChildren(
                            mapOf(
                                "character" to selectCharacter
                            )
                        )
                        alertDialog.dismiss()
                        Handler(Looper.getMainLooper()).postDelayed({
                            var uid = auth?.uid.toString()
                            Patch(uid)
                        }, 2000)
                    }

                    alertDialog.show()

                }

                1 -> {
                    refPowerMenu.dismiss()
                    Firebase.auth.signOut()
                    var intent = Intent(activity, LoginActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }

                2 -> {
                    refPowerMenu.dismiss()
                    val deleteDialogView = layoutInflater.inflate(R.layout.delete_dialog, null)
                    val deleteDialog = AlertDialog.Builder(requireActivity())
                        .setView(deleteDialogView)
                        .create()

                    var yesBtn = deleteDialogView.findViewById<Button>(R.id.delete_yes_btn)
                    var noBtn = deleteDialogView.findViewById<Button>(R.id.delete_no_btn)

                    yesBtn.setOnClickListener {
                        email = auth!!.currentUser?.email
                        // storage 인스턴스 생성
                        val storage = Firebase.storage
                        // storage 참조
                        val storageRef = storage.getReference("image")
                        // storage에서 삭제 할 파일명
                        val fileName = email.toString()
                        Log.d("스토리지", fileName)
                        val mountainsRef = storageRef.child("${fileName}.jpg")
                        mountainsRef.delete()
                        database.child("user").child(auth!!.uid!!).removeValue()

                        FirebaseFirestore.getInstance()
                            .collection("EternalReturnInfo")
                            .document(auth!!.uid!!)
                            .delete()
                        val user = Firebase.auth.currentUser!!
                        user.delete()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("계정삭제", "User account deleted.")
                                }
                            }
                        deleteDialog.dismiss()
                        var intent = Intent(activity, LoginActivity::class.java)
                        (context as MainActivity).finish()
                        startActivity(intent)
                    }
                    noBtn.setOnClickListener {
                        deleteDialog.dismiss()
                    }
                    deleteDialog.show()
                }

                else -> {
                    val pwChangeDialogView =
                        layoutInflater.inflate(R.layout.myprofile_pwchange_dialog, null)
                    val pwChangeDialog = AlertDialog.Builder(requireActivity())
                        .setView(pwChangeDialogView)
                        .create()

                    val changeBtn =
                        pwChangeDialogView.findViewById<Button>(R.id.myprofile_pwChange_btn)
                    val firstPw =
                        pwChangeDialogView.findViewById<EditText>(R.id.myprofile_pwChange_et)
                    val secondPw =
                        pwChangeDialogView.findViewById<EditText>(R.id.myprofile_pwChange2_et)

                    changeBtn.setOnClickListener {

                        val pw = firstPw.text.toString()
                        val pwCheck = secondPw.text.toString()
                        if (pw.isNotEmpty() && pwCheck.isNotEmpty()) {
                            if (pw == pwCheck) {
                                PwChange(pw, pwCheck)
                                pwChangeDialog.dismiss()
                            } else {
                                MotionToast.darkColorToast(
                                    requireActivity(), "CHECK", "동일한 비밀번호를 입력하세요",
                                    MotionToastStyle.WARNING,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.SHORT_DURATION,
                                    font = null
                                )
                            }
                        } else {
                            MotionToast.darkColorToast(
                                requireActivity(), "CHECK", "비밀번호를 입력하세요",
                                MotionToastStyle.WARNING,
                                MotionToast.GRAVITY_BOTTOM,
                                MotionToast.SHORT_DURATION,
                                font = null
                            )
                        }

                    }
                    pwChangeDialog.show()
                }
            }
        }
    }
}

