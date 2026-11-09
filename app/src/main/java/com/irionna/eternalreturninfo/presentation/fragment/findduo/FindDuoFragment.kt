package com.irionna.eternalreturninfo.presentation.fragment.findduo

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.data.repository.BlockedUsers
import com.irionna.eternalreturninfo.data.repository.DuoSnapshots
import com.irionna.eternalreturninfo.databinding.FindDuoFragmentBinding
import com.irionna.eternalreturninfo.presentation.activity.board.BoardDialog
import com.irionna.eternalreturninfo.presentation.activity.board.DialogListener
import com.irionna.eternalreturninfo.presentation.activity.chat.ChatActivity
import com.irionna.eternalreturninfo.presentation.adapter.findduo.DuoAdapter
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.util.BlockDialog
import com.irionna.eternalreturninfo.util.CharacterPicker
import com.irionna.eternalreturninfo.util.OptionSheet
import com.irionna.eternalreturninfo.util.StatsPanel
import com.irionna.eternalreturninfo.util.Tiers
import com.irionna.eternalreturninfo.util.UserReportDialog
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 듀오 찾기: 모집글 목록(카드)과 필터, 모집 등록.
 *  - 모집글은 사용자 정보(user/{uid})의 서버·성별·티어·모스트·시각·전적 요약 칸이다. 3일이 지난 글은 목록에서 숨긴다(내 글은 그대로).
 *  - 카드는 저장된 전적 요약만 보여준다. 내 글의 요약은 앱을 켜고 이 화면을 처음 볼 때 한 번 새로 고친다.
 */
class FindDuoFragment : Fragment() {
    companion object {
        fun newInstance() = FindDuoFragment()
        private const val TAG = "FindDuo"
        private const val EXPIRE_MS = 3L * 24 * 60 * 60 * 1000
        /** 이번 앱 실행 중 내 모집글의 전적 요약을 이미 새로 고쳤는지 */
        private var mineRefreshed = false
    }

    private var _binding: FindDuoFragmentBinding? = null
    private val binding get() = requireNotNull(_binding)
    private lateinit var mDbRef: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private var refDb: DatabaseReference? = null
    private var refEventListener: ValueEventListener? = null

    // 필터 (null = 전체)
    private var fServer: String? = null
    private var fTier: String? = null
    private var fMost: String? = null
    private var fGender: String? = null

    private var allDuoUsers: List<ERModel> = emptyList()

    private val adapter: DuoAdapter by lazy {
        DuoAdapter(
            myUid = { mAuth.currentUser?.uid },
            onChat = { item ->
                BoardDialog(requireContext(), item.name ?: "", object : DialogListener {
                    override fun onOKButtonClicked() {
                        startActivity(ChatActivity.newIntent(requireContext(), item))
                    }
                }).show()
            },
            onRemoveMine = { confirmRemoveMine() },
            onMore = { item, anchor ->
                PopupMenu(requireContext(), anchor).apply {
                    menu.add(0, 1, 0, R.string.duo_menu_report)
                    menu.add(0, 2, 1, R.string.duo_menu_block)
                    setOnMenuItemClickListener {
                        when (it.itemId) {
                            1 -> UserReportDialog.show(requireContext(), item.uid, "post", "duo:${item.uid}")
                            else -> BlockDialog.show(requireContext(), item.uid, item.name)
                        }
                        true
                    }
                }.show()
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FindDuoFragmentBinding.inflate(inflater, container, false)
        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().reference
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.findduoRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.findduoRecyclerview.adapter = adapter
        binding.findduoRecyclerview.itemAnimator = null
        binding.findduoRegisterBtn.setOnClickListener { openRegister() }
        BlockedUsers.blocked.observe(viewLifecycleOwner) { render() }
        refreshChips()
    }

    override fun onResume() {
        super.onResume()
        startListening()
    }

    override fun onPause() {
        super.onPause()
        refEventListener?.let { refDb?.removeEventListener(it) }
        refEventListener = null
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ---- 서버에서 모집글 받기 ----

    private fun startListening() {
        if (refEventListener != null) return
        val ref = mDbRef.child("user")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = ArrayList<ERModel>()
                for (child in snapshot.children) {
                    val user = child.getValue(ERModel::class.java) ?: continue
                    if (!user.server.isNullOrEmpty() && !user.name.isNullOrEmpty() &&
                        !user.gender.isNullOrEmpty() && !user.tier.isNullOrEmpty()
                    ) list.add(user)
                }
                allDuoUsers = list
                if (_binding == null) return
                render()
                refreshMineSnapshotOnce()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "모집글을 불러오지 못했어요: $error")
            }
        }
        refDb = ref
        refEventListener = listener
        ref.addValueEventListener(listener)
    }

    /** 3일 지난 글 숨김, 차단한 사용자 숨김, 필터 적용, 내 글을 맨 위에 두고 나머지는 최신순. */
    private fun render() {
        val b = _binding ?: return
        val myUid = mAuth.currentUser?.uid
        val now = System.currentTimeMillis()

        val visible = allDuoUsers.filter { u ->
            val mine = u.uid == myUid
            if (!mine && BlockedUsers.isBlocked(u.uid)) return@filter false
            if (!mine) {
                val posted = u.timestamp?.toLongOrNull() ?: return@filter false // 시각이 없는 아주 오래된 글은 숨김
                if (now - posted > EXPIRE_MS) return@filter false
            }
            (fServer == null || u.server == fServer) &&
                (fTier == null || Tiers.displayName(u.tier) == fTier) &&
                (fMost == null || u.most == fMost) &&
                (fGender == null || u.gender == fGender)
        }.sortedWith(
            compareByDescending<ERModel> { it.uid == myUid }
                .thenByDescending { it.timestamp?.toLongOrNull() ?: 0L }
        )

        adapter.submitList(visible)
        b.findduoTotalNumber.text = getString(R.string.duo_count, visible.size)
        b.findduoEmpty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE

        val hasMine = allDuoUsers.any { it.uid == myUid }
        b.findduoRegisterBtn.text = getString(if (hasMine) R.string.duo_register_edit else R.string.duo_register)
    }

    // ---- 필터 칩 ----

    private fun refreshChips() {
        val b = _binding ?: return
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val row = b.findduoChips
        row.removeAllViews()

        fun chip(label: String, value: String?, onClick: () -> Unit) {
            val active = value != null
            row.addView(TextView(ctx).apply {
                text = if (active) "$label · $value" else "$label ▾"
                textSize = 13f
                gravity = Gravity.CENTER
                minHeight = (40 * dp).toInt()
                setPadding((14 * dp).toInt(), 0, (14 * dp).toInt(), 0)
                setBackgroundResource(if (active) R.drawable.shape_chip_active else R.drawable.shape_chip)
                setTextColor(ContextCompat.getColor(ctx, if (active) R.color.brand_mint else R.color.white))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setOnClickListener { onClick() }
            })
        }

        val all = getString(R.string.duo_filter_all)
        chip(getString(R.string.duo_filter_server), fServer) {
            OptionSheet.show(requireActivity(), getString(R.string.duo_filter_server),
                resources.getStringArray(R.array.server).drop(1), fServer, all) { fServer = it; filtersChanged() }
        }
        chip(getString(R.string.duo_filter_tier), fTier) {
            OptionSheet.show(requireActivity(), getString(R.string.duo_filter_tier),
                Tiers.SELECTABLE, fTier, all) { fTier = it; filtersChanged() }
        }
        chip(getString(R.string.duo_filter_most), fMost) {
            val names = resources.getStringArray(R.array.characterName)
            CharacterPicker.show(requireActivity(), names, names.indexOf(fMost).coerceAtLeast(0), all) { picked ->
                fMost = if (picked >= 1) names[picked] else null
                filtersChanged()
            }
        }
        chip(getString(R.string.duo_filter_gender), fGender) {
            OptionSheet.show(requireActivity(), getString(R.string.duo_filter_gender),
                resources.getStringArray(R.array.gender).drop(1), fGender, all) { fGender = it; filtersChanged() }
        }
    }

    private fun filtersChanged() {
        refreshChips()
        render()
    }

    // ---- 등록 / 내리기 ----

    private fun openRegister() {
        DuoRegisterSheet(requireActivity(), viewLifecycleOwner.lifecycleScope) { render() }.show()
    }

    private fun confirmRemoveMine() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.duo_remove_title)
            .setPositiveButton(R.string.duo_remove_ok) { _, _ -> removeMine() }
            .setNegativeButton(R.string.block_cancel, null)
            .show()
    }

    /** 내 사용자 정보에서 모집 칸만 비운다 (닉네임, 프로필 등은 그대로). */
    private fun removeMine() {
        val uid = mAuth.currentUser?.uid ?: return
        val clear = mapOf<String, Any?>(
            "server" to null, "gender" to null, "tier" to null, "most" to null,
            "timestamp" to null, "winrate" to null, "avgrank" to null, "recent" to null, "tierAuto" to null
        )
        mDbRef.child("user").child(uid).updateChildren(clear)
            .addOnSuccessListener { context?.let { Toast.makeText(it, R.string.duo_removed, Toast.LENGTH_SHORT).show() } }
    }

    // ---- 내 모집글의 전적 요약 새로 고침 (앱 실행당 한 번) ----

    private fun refreshMineSnapshotOnce() {
        if (mineRefreshed) return
        val uid = mAuth.currentUser?.uid ?: return
        val mine = allDuoUsers.firstOrNull { it.uid == uid } ?: return
        if (mine.tierAuto != true) return // 직접 입력한 티어는 새로 고칠 전적이 없다
        mineRefreshed = true
        val name = mine.name ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val snap = DuoSnapshots.fetch(name) ?: return@launch
            val update = mapOf<String, Any?>(
                "tier" to snap.tier,
                "winrate" to String.format(Locale.US, "%.1f", snap.winRate * 100),
                "avgrank" to String.format(Locale.US, "%.1f", snap.avgRank),
                "recent" to snap.recent.joinToString(",")
            )
            // timestamp(올린 시각)는 그대로 둔다
            mDbRef.child("user").child(uid).updateChildren(update)
        }
    }
}
