package com.irionna.eternalreturninfo.presentation.fragment.findduo

import com.irionna.eternalreturninfo.util.AppToast
import android.app.Activity
import android.app.Dialog
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.repository.DuoSnapshots
import com.irionna.eternalreturninfo.databinding.DuoRegisterSheetBinding
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.util.CharacterPicker
import com.irionna.eternalreturninfo.util.StatsPanel
import com.irionna.eternalreturninfo.util.Tiers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 듀오 모집 등록 시트: 서버·성별은 버튼, 모스트는 얼굴 선택 시트, 티어는 게임 전적에서 자동으로 가져온다.
 * 게임 닉네임을 찾지 못하면 티어만 직접 고르고, 이 경우 승률·최근 경기는 비워 둔다.
 * 등록하면 내 사용자 정보(user/{uid})에 한 번에 저장한다.
 */
class DuoRegisterSheet(
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val onDone: () -> Unit
) {
    private lateinit var b: DuoRegisterSheetBinding
    private lateinit var dialog: Dialog

    private var server: String? = null
    private var gender: String? = null
    private var mostIndex = 0
    private var snapshot: DuoSnapshots.Snapshot? = null
    private var manualTier: String? = null
    private var tierResolved = false // 전적 조회가 끝났는지

    fun show() {
        b = DuoRegisterSheetBinding.inflate(LayoutInflater.from(activity))
        dialog = Dialog(activity, R.style.ConsentSheetDialog)
        dialog.setContentView(b.root)
        val height = (activity.resources.displayMetrics.heightPixels * 0.82f).toInt()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height)
            setGravity(Gravity.BOTTOM)
        }

        val servers = activity.resources.getStringArray(R.array.server).drop(1)
        val genders = activity.resources.getStringArray(R.array.gender).drop(1)
        fillPills(b.regServerRow, servers, { server }) { server = it }
        fillPills(b.regGenderRow, genders, { gender }) { gender = it }

        b.regMostRow.setOnClickListener {
            val names = activity.resources.getStringArray(R.array.characterName)
            CharacterPicker.show(activity, names, mostIndex) { picked ->
                mostIndex = picked
                showMost()
            }
        }
        b.regSubmit.setOnClickListener { submit() }

        loadSnapshot()
        dialog.show()
    }

    // ---- 선택 칸 ----

    private fun fillPills(row: LinearLayout, items: List<String>, current: () -> String?, onPick: (String) -> Unit) {
        val dp = activity.resources.displayMetrics.density
        fun render() {
            row.removeAllViews()
            items.forEachIndexed { i, label ->
                val on = current() == label
                row.addView(TextView(activity).apply {
                    text = label
                    gravity = Gravity.CENTER
                    textSize = 14f
                    setBackgroundResource(if (on) R.drawable.shape_pill_on else R.drawable.shape_pill)
                    setTextColor(ContextCompat.getColor(activity, if (on) R.color.brand_mint else R.color.white))
                    if (on) setTypeface(typeface, Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, (44 * dp).toInt(), 1f).apply {
                        if (i > 0) marginStart = (8 * dp).toInt()
                    }
                    setOnClickListener {
                        onPick(label)
                        render()
                    }
                })
            }
        }
        render()
    }

    private fun showMost() {
        val names = activity.resources.getStringArray(R.array.characterName)
        if (mostIndex >= 1) {
            b.regMostFace.visibility = View.VISIBLE
            CharacterPicker.loadFace(b.regMostFace, mostIndex)
            b.regMostName.text = names[mostIndex]
            b.regMostName.setTextColor(ContextCompat.getColor(activity, R.color.white))
        } else {
            b.regMostFace.visibility = View.GONE
            b.regMostName.text = activity.getString(R.string.duo_reg_most_hint)
            b.regMostName.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
        }
    }

    // ---- 티어: 전적에서 자동, 실패하면 직접 ----

    private fun loadSnapshot() {
        scope.launch {
            val name = StatsPanel.waitForNickname(5000) { BoardSingletone.LoginUser().name }
            val snap = if (name.isNullOrBlank()) null else DuoSnapshots.fetch(name)
            snapshot = snap
            tierResolved = true
            if (snap != null) {
                val text = if (snap.rp > 0) {
                    activity.getString(R.string.duo_reg_tier_auto, snap.tier, String.format(Locale.US, "%,d", snap.rp))
                } else activity.getString(R.string.duo_reg_tier_auto_unranked, snap.tier)
                b.regTierText.text = text
                b.regTierText.setTextColor(ContextCompat.getColor(activity, R.color.brand_mint))
            } else {
                b.regTierText.text = activity.getString(R.string.duo_reg_tier_manual)
                b.regTierText.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                b.regTierScroll.visibility = View.VISIBLE
                fillManualTiers()
            }
        }
    }

    private fun fillManualTiers() {
        val dp = activity.resources.displayMetrics.density
        fun render() {
            b.regTierRow.removeAllViews()
            Tiers.SELECTABLE.forEachIndexed { i, label ->
                val on = manualTier == label
                b.regTierRow.addView(TextView(activity).apply {
                    text = label
                    gravity = Gravity.CENTER
                    textSize = 13f
                    setPadding((14 * dp).toInt(), 0, (14 * dp).toInt(), 0)
                    setBackgroundResource(if (on) R.drawable.shape_pill_on else R.drawable.shape_pill)
                    setTextColor(ContextCompat.getColor(activity, if (on) R.color.brand_mint else R.color.white))
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (40 * dp).toInt()).apply {
                        if (i > 0) marginStart = (8 * dp).toInt()
                    }
                    setOnClickListener {
                        manualTier = label
                        b.regTierText.text = activity.getString(R.string.duo_reg_tier_picked, label)
                        b.regTierText.setTextColor(ContextCompat.getColor(activity, R.color.white))
                        render()
                    }
                })
            }
        }
        render()
    }

    // ---- 저장 ----

    private fun submit() {
        val names = activity.resources.getStringArray(R.array.characterName)
        val tier = snapshot?.tier ?: manualTier
        val msg = when {
            server == null -> R.string.duo_reg_need_server
            gender == null -> R.string.duo_reg_need_gender
            mostIndex < 1 -> R.string.duo_reg_need_most
            tier == null -> R.string.duo_reg_need_tier
            else -> null
        }
        if (msg != null) {
            AppToast.show(activity, msg, AppToast.Kind.WARNING, overDialog = true)
            return
        }
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val snap = snapshot
        // 전적에서 가져온 경우에만 승률·평균 순위·최근 경기를 저장한다. 직접 입력한 티어는 이 값들을 비운다(null = 삭제).
        val data = mapOf<String, Any?>(
            "server" to server,
            "gender" to gender,
            "most" to names[mostIndex],
            "tier" to tier,
            "tierAuto" to (snap != null),
            "winrate" to snap?.let { String.format(Locale.US, "%.1f", it.winRate * 100) },
            "avgrank" to snap?.let { String.format(Locale.US, "%.1f", it.avgRank) },
            "recent" to snap?.recent?.joinToString(","),
            "timestamp" to System.currentTimeMillis().toString()
        )
        b.regSubmit.isEnabled = false
        FirebaseDatabase.getInstance().reference.child("user").child(uid).updateChildren(data)
            .addOnSuccessListener {
                dialog.dismiss()
                AppToast.show(activity, R.string.duo_registered, AppToast.Kind.SUCCESS)
                onDone()
            }
            .addOnFailureListener {
                b.regSubmit.isEnabled = true
                AppToast.show(activity, R.string.duo_register_failed, AppToast.Kind.ERROR, overDialog = true)
            }
    }
}
