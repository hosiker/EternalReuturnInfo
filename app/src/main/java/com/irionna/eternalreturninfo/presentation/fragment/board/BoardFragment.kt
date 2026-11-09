package com.irionna.eternalreturninfo.presentation.fragment.board

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.BoardModel
import com.irionna.eternalreturninfo.data.repository.BlockedUsers
import com.irionna.eternalreturninfo.databinding.BoardFragmentBinding
import com.irionna.eternalreturninfo.presentation.activity.board.BoardAddActivity
import com.irionna.eternalreturninfo.presentation.activity.board.BoardDeletedActivity
import com.irionna.eternalreturninfo.presentation.activity.board.BoardPostActivity
import com.irionna.eternalreturninfo.presentation.activity.board.BoardSearchActivity
import com.irionna.eternalreturninfo.presentation.adapter.board.BoardRecyclerViewAdapter
import com.irionna.eternalreturninfo.presentation.viewmodel.BoardListViewModel
import com.irionna.eternalreturninfo.retrofit.FBRef

/**
 * 게시판 목록: 카테고리 칩(전체·공지·질문·공략·일상)으로 한 개의 목록을 거른다.
 * 공지는 "전체"에서 맨 위에 고정하고, 나머지는 최신순이다.
 */
class BoardFragment : Fragment() {
    companion object {
        fun newInstance() = BoardFragment()
        private const val NOTICE = "공지"
    }

    private var _binding: BoardFragmentBinding? = null
    private val binding get() = _binding!!

    private val boardViewModel: BoardListViewModel by activityViewModels()
    private val listAdapter by lazy { BoardRecyclerViewAdapter() }

    /** 선택한 카테고리 (null = 전체) */
    private var category: String? = null

    // 글 작성 결과: 목록을 다시 불러온다
    private val addBoardLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) refresh()
        }

    // 글 상세에서 돌아온 결과: 수정되었으면 목록의 그 글을 바꾸고, 신고되어 가려진 글이면 목록에서 뺀다
    private val postLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val updated: BoardModel? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra("updateBoard", BoardModel::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra("updateBoard")
            }
            if (updated != null) {
                if (updated.category == "report") boardViewModel.removeBoard(updated)
                else boardViewModel.updateBoard(updated)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BoardFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun initView() = with(binding) {
        boardRv.layoutManager = LinearLayoutManager(requireContext())
        boardRv.adapter = listAdapter
        boardRv.itemAnimator = null

        // 사용자를 차단하거나 해제하면 목록을 다시 불러와서 반영한다 (처음 보일 때도 한 번 불러온다)
        BlockedUsers.blocked.observe(viewLifecycleOwner) { refresh() }
        boardViewModel.boardList.observe(viewLifecycleOwner) { render(it) }

        listAdapter.setOnItemClickListener(object : BoardRecyclerViewAdapter.OnItemClickListener {
            override fun onItemClick(boardItem: BoardModel) = openPost(boardItem)
        })

        boardFab.setOnClickListener {
            addBoardLauncher.launch(Intent(requireContext(), BoardAddActivity::class.java))
        }
        boardIbProfile.setOnClickListener {
            startActivity(Intent(requireContext(), BoardSearchActivity::class.java))
        }
        boardSwipeRefreshLayout.setOnRefreshListener {
            refresh()
            boardSwipeRefreshLayout.isRefreshing = false
        }
        refreshChips()
    }

    // ---- 목록 ----

    private fun render(all: List<BoardModel>) {
        val b = _binding ?: return
        val shown = all.filter { category == null || it.category == category }
        // 공지는 맨 위(전체에서만), 나머지는 최신순
        val (notices, others) = shown.partition { it.category == NOTICE }
        val sorted = notices.sortedByDescending { it.date } + others.sortedByDescending { it.date }
        listAdapter.submitList(sorted)
        b.boardEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openPost(boardItem: BoardModel) {
        Log.i("BoardOpen", "카드 눌림 id='${boardItem.id}' title='${boardItem.title}'")
        if (boardItem.id.isEmpty()) {
            android.widget.Toast.makeText(requireContext(), R.string.post_open_failed, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        // 열기 전에 글이 아직 있는지 한 번 확인한다 (조회수는 글 화면이 열릴 때 올린다)
        FBRef.postRef.child(boardItem.id).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.i("BoardOpen", "존재 확인 결과 exists=${dataSnapshot.exists()} isAdded=$isAdded")
                if (!isAdded) return
                if (!dataSnapshot.exists()) {
                    startActivity(Intent(requireContext(), BoardDeletedActivity::class.java))
                } else {
                    val intent = Intent(requireContext(), BoardPostActivity::class.java)
                    intent.putExtra("ID", boardItem.id)
                    postLauncher.launch(intent)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("BoardOpen", "글 확인 실패: ${databaseError.message}")
                if (isAdded) android.widget.Toast.makeText(requireContext(), R.string.post_open_failed, android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ---- 카테고리 칩 ----

    private fun refreshChips() {
        val b = _binding ?: return
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val row = b.boardChips
        row.removeAllViews()
        listOf(
            null to R.string.board_total,
            "공지" to R.string.board_notice,
            "질문" to R.string.board_ask,
            "공략" to R.string.board_tips,
            "일상" to R.string.board_daily
        ).forEach { (value, label) ->
            val on = category == value
            row.addView(TextView(ctx).apply {
                text = getString(label)
                textSize = 13f
                gravity = Gravity.CENTER
                minHeight = (40 * dp).toInt()
                setPadding((14 * dp).toInt(), 0, (14 * dp).toInt(), 0)
                setBackgroundResource(if (on) R.drawable.shape_chip_active else R.drawable.shape_chip)
                setTextColor(ContextCompat.getColor(ctx, if (on) R.color.brand_mint else R.color.white))
                if (on) setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (8 * dp).toInt()
                }
                setOnClickListener {
                    category = value
                    refreshChips()
                    render(boardViewModel.boardList.value.orEmpty())
                }
            })
        }
    }

    // ---- 서버에서 글 목록 받기 ----

    fun refresh() {
        FBRef.postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 응답이 늦게 도착했을 때 탭 이동 등으로 화면이 이미 분리되어 있으면 아무것도 하지 않는다.
                if (!isAdded) return
                val list = mutableListOf<BoardModel>()
                for (data in snapshot.children) {
                    val board = data.getValue(BoardModel::class.java)?.let { b -> if (data.key != null && b.id != data.key) b.copy(id = data.key!!) else b }
                    // 내가 신고한 글과 차단한 사용자의 글은 보이지 않게 한다
                    if (board != null && board.report.values.none { r -> r.uid == FBRef.auth.uid } && !BlockedUsers.isBlocked(board.author)) {
                        list.add(board)
                    }
                }
                boardViewModel.initBoard(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("error", "firebase data loading failed")
            }
        })
    }
}
