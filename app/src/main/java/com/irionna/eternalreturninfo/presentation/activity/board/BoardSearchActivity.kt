package com.irionna.eternalreturninfo.presentation.activity.board

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.BoardModel
import com.irionna.eternalreturninfo.data.repository.BlockedUsers
import com.irionna.eternalreturninfo.databinding.BoardSearchActivityBinding
import com.irionna.eternalreturninfo.presentation.adapter.board.BoardRecyclerViewAdapter
import com.irionna.eternalreturninfo.retrofit.FBRef
import com.irionna.eternalreturninfo.util.RecentSearches
import com.irionna.eternalreturninfo.util.setContentViewEdgeToEdge

/**
 * 게시판 검색: 제목·내용 어디든 검색어가 들어 있는 글을 찾는다 (대소문자 구분 없음).
 * 글 전체를 한 번 받아 와서 기기에서 걸러낸다 (서버 쿼리는 "앞부분이 같은 글"만 찾을 수 있어서 쓰지 않는다).
 * 신고한 글, 차단한 사용자의 글은 결과에서 뺀다. 최근 검색어는 이 기기에만 저장한다.
 */
class BoardSearchActivity : AppCompatActivity() {

    private lateinit var binding: BoardSearchActivityBinding
    private val listAdapter by lazy { BoardRecyclerViewAdapter() }

    // 한 번 받은 글 목록은 잠깐 다시 쓴다
    private var cache: List<BoardModel>? = null
    private var cacheAt = 0L
    private var lastQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BoardSearchActivityBinding.inflate(layoutInflater)
        setContentViewEdgeToEdge(binding.root)

        with(binding) {
            boardSearchRv.adapter = listAdapter
            boardSearchRv.layoutManager = LinearLayoutManager(this@BoardSearchActivity)

            boardSearchIbBack.setOnClickListener { finish() }
            boardSearchIbClear.setOnClickListener {
                boardSearchEtSearch.setText("")
                showRecent()
            }
            boardSearchEtSearch.doAfterTextChanged {
                boardSearchIbClear.visibility = if (it.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
                if (it.isNullOrBlank()) showRecent()
            }
            boardSearchEtSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    search(boardSearchEtSearch.text.toString())
                    true
                } else false
            }
            boardSearchRecentClearAll.setOnClickListener {
                RecentSearches.clear(this@BoardSearchActivity, RecentSearches.SCOPE_BOARD)
                showRecent()
            }
        }

        listAdapter.setOnItemClickListener(object : BoardRecyclerViewAdapter.OnItemClickListener {
            override fun onItemClick(boardItem: BoardModel) = openPost(boardItem)
        })

        showRecent()
    }

    // ───────── 검색 전: 최근 검색어 ─────────

    private fun showRecent(): Unit = with(binding) {
        lastQuery = ""
        listAdapter.submitList(emptyList())
        listAdapter.setHighlight("")
        boardSearchTvPostCount.visibility = View.GONE
        boardSearchProgress.visibility = View.GONE
        val recent = RecentSearches.get(this@BoardSearchActivity, RecentSearches.SCOPE_BOARD)
        boardSearchRecentLayout.visibility = if (recent.isEmpty()) View.GONE else View.VISIBLE
        boardSearchTvResult.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
        boardSearchTvResult.setText(R.string.search_guide)

        boardSearchRecentList.removeAllViews()
        val dp = resources.displayMetrics.density
        recent.forEach { word ->
            val row = LinearLayout(this@BoardSearchActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = (48 * dp).toInt()
                setBackgroundResource(android.R.color.transparent)
            }
            row.addView(TextView(this@BoardSearchActivity).apply {
                text = word
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.white))
                layoutParams = LinearLayout.LayoutParams(0, (48 * dp).toInt(), 1f)
                gravity = Gravity.CENTER_VERTICAL
                setOnClickListener {
                    boardSearchEtSearch.setText(word)
                    boardSearchEtSearch.setSelection(word.length)
                    search(word)
                }
            })
            row.addView(TextView(this@BoardSearchActivity).apply {
                text = "✕"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (48 * dp).toInt())
                contentDescription = getString(R.string.block_cancel)
                setOnClickListener {
                    RecentSearches.remove(this@BoardSearchActivity, word, RecentSearches.SCOPE_BOARD)
                    showRecent()
                }
            })
            boardSearchRecentList.addView(row)
        }
    }

    // ───────── 검색 ─────────

    private fun search(raw: String) {
        val query = raw.trim()
        if (query.isEmpty()) {
            showRecent()
            return
        }
        RecentSearches.add(this, query, RecentSearches.SCOPE_BOARD)
        hideKeyboard()
        lastQuery = query
        binding.boardSearchRecentLayout.visibility = View.GONE

        val cached = cache
        if (cached != null && System.currentTimeMillis() - cacheAt < CACHE_MS) {
            showResult(query, cached)
            return
        }
        binding.boardSearchProgress.visibility = View.VISIBLE
        binding.boardSearchTvResult.visibility = View.GONE
        binding.boardSearchTvPostCount.visibility = View.GONE
        FBRef.postRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed) return
                val posts = snapshot.children.mapNotNull {
                    runCatching { it.getValue(BoardModel::class.java) }.getOrNull()
                }
                cache = posts
                cacheAt = System.currentTimeMillis()
                if (lastQuery == query) showResult(query, posts)
            }

            override fun onCancelled(error: DatabaseError) {
                if (isFinishing || isDestroyed || lastQuery != query) return
                binding.boardSearchProgress.visibility = View.GONE
                binding.boardSearchTvResult.visibility = View.VISIBLE
                binding.boardSearchTvResult.setText(R.string.search_failed)
            }
        })
    }

    private fun showResult(query: String, posts: List<BoardModel>): Unit = with(binding) {
        val me = FBRef.auth.uid
        val found = posts
            .filter {
                it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
            }
            .filter { post -> post.report.values.none { it.uid == me } && !BlockedUsers.isBlocked(post.author) }
            .sortedByDescending { it.date }

        boardSearchProgress.visibility = View.GONE
        listAdapter.setHighlight(query)
        listAdapter.submitList(found)
        boardSearchTvPostCount.visibility = View.VISIBLE
        boardSearchTvPostCount.text = getString(R.string.search_result_count, found.size)
        boardSearchTvResult.visibility = if (found.isEmpty()) View.VISIBLE else View.GONE
        boardSearchTvResult.setText(R.string.search_no_result)
    }

    private fun openPost(boardItem: BoardModel) {
        // 열기 전에 글이 아직 있는지 한 번 확인한다 (조회수는 글 화면이 열릴 때 올린다)
        FBRef.postRef.child(boardItem.id).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed) return
                if (snapshot.exists()) {
                    startActivity(Intent(this@BoardSearchActivity, BoardPostActivity::class.java).putExtra("ID", boardItem.id))
                } else {
                    startActivity(Intent(this@BoardSearchActivity, BoardDeletedActivity::class.java))
                    cache = null
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.boardSearchEtSearch.windowToken, 0)
    }

    private companion object {
        const val CACHE_MS = 60_000L
    }
}
