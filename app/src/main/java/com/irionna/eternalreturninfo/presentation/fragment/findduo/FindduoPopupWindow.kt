package com.irionna.eternalreturninfo.presentation.fragment.findduo

import android.content.ContentValues
import com.google.firebase.database.FirebaseDatabase
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupWindow
import android.widget.Toast
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.FindduoPopupWindowActivityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

class FindduoPopupWindow(private val context: Context) {
    private var popupWindow: PopupWindow? = null
    private var _binding: FindduoPopupWindowActivityBinding? = null
    private val binding get() = _binding!!
    private lateinit var mDbRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth

    fun showPopup(anchorView: View) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        _binding = FindduoPopupWindowActivityBinding.inflate(inflater, null, false)
        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().reference

        popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow?.apply {
            elevation = 10.0f
            val alphaColor = Color.argb(80, 0, 0, 0)
            setBackgroundDrawable(ColorDrawable(alphaColor))
            isOutsideTouchable = true
            animationStyle = android.R.style.Animation_InputMethod
        }


        val serverList = context.resources.getStringArray(R.array.server)
        val serverAdpater = ArrayAdapter<String>(
            context,
            R.layout.findduo_spinner,
            R.id.findduo_spinner_tv,
            serverList
        )
        var selectServer: String? = "선택"

        binding.findduoServerBtn.adapter = serverAdpater

        binding.findduoServerBtn.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectServer = serverList[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectServer = "선택"
            }
        }

        val tierList = context.resources.getStringArray(R.array.tier)
        val tierAdapter =
            ArrayAdapter<String>(context, R.layout.findduo_spinner, R.id.findduo_spinner_tv, tierList)
        var selectTier: String? = "선택"

        binding.findduoTierBtn.adapter = tierAdapter

        binding.findduoTierBtn.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectTier = tierList[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectServer = "선택"
                }
            }

        val genderList = context.resources.getStringArray(R.array.gender)
        val genderAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            context,
            R.layout.findduo_spinner,
            R.id.findduo_spinner_tv,
            genderList
        )
        var selectGender : String? = "선택"

        binding.findduoGenderBtn.adapter = genderAdapter

        binding.findduoGenderBtn.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectGender = genderList[position]
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectServer = "선택"
                }
            }

        val mostList = context.resources.getStringArray(R.array.characterName)
        val mostAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            context,
            R.layout.findduo_spinner,
            R.id.findduo_spinner_tv,
            mostList
        )
        var selectMost: String? = "선택"

        binding.findduoMostBtn.adapter = mostAdapter

        binding.findduoMostBtn.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectMost = mostList[position]
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectServer = "선택"
                }

            }

        binding.findduoYesBtn.setOnClickListener {
            if (selectServer != "선택" && selectTier != "선택" && selectGender != "선택" && selectMost != "선택") {
                updateUserInFirebase(selectServer, "server")
                updateUserInFirebase(selectTier, "tier")
                updateUserInFirebase(selectGender, "gender")
                updateUserInFirebase(selectMost, "most")
                addTimestampToFirebase()
                dismissPopup()
            } else {
                Toast.makeText(context, "모든 옵션을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        popupWindow?.showAtLocation(anchorView, Gravity.CENTER, 0, 0)
    }

    private fun addTimestampToFirebase() {
        val timestamp = System.currentTimeMillis()
        updateUserInFirebase(timestamp.toString(), "timestamp")
    }

    fun dismissPopup() {
        popupWindow?.dismiss()
    }

    private fun updateUserInFirebase(finalSelection: String?, s: String) {
        val userId = mAuth.currentUser?.uid

        if (userId == null) {
            Log.w(ContentValues.TAG, "User UID is null")
            return
        }

        val updateData = mapOf(
            s to finalSelection
        )

        val databasePath = "user/$userId"
        mDbRef.child(databasePath).updateChildren(updateData)
    }
}