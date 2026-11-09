package com.irionna.eternalreturninfo.presentation.activity.board

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.BoardModel
import com.irionna.eternalreturninfo.data.model.CommentModel
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.data.model.ReportModel
import com.irionna.eternalreturninfo.data.repository.BlockedUsers
import com.irionna.eternalreturninfo.databinding.BoardPostActivityBinding
import com.irionna.eternalreturninfo.presentation.activity.chat.ChatActivity
import com.irionna.eternalreturninfo.presentation.adapter.board.BoardCommentRecyclerViewAdpater
import com.irionna.eternalreturninfo.presentation.viewmodel.BoardListViewModel
import com.irionna.eternalreturninfo.presentation.viewmodel.BoardListViewModelFactory
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.retrofit.FBRef
import com.irionna.eternalreturninfo.util.ActionSheet
import com.irionna.eternalreturninfo.util.AppDialog
import com.irionna.eternalreturninfo.util.AuthorCache
import com.irionna.eternalreturninfo.util.BlockDialog
import com.irionna.eternalreturninfo.util.ProfileImage
import com.irionna.eternalreturninfo.util.UserReportDialog
import com.irionna.eternalreturninfo.util.setContentViewEdgeToEdge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 게시글 상세: 글 + 댓글 + 하단 댓글 입력. 메뉴는 아래에서 올라오는 시트로 연다. */
class BoardPostActivity : AppCompatActivity() {

    private lateinit var binding: BoardPostActivityBinding

    private val listAdapter by lazy { BoardCommentRecyclerViewAdpater { board?.author } }

    private val boardViewModel by lazy {
        ViewModelProvider(this, BoardListViewModelFactory()).get(BoardListViewModel::class.java)
    }

    private var board: BoardModel? = null
    private var id: String = ""

    // 서버에서 받은 댓글 전체 (신고한 댓글, 차단한 사용자의 댓글은 화면에 그릴 때 뺀다)
    private var allComments: List<CommentModel> = emptyList()

    // 수정 중인 댓글 (null 이면 새 댓글 작성)
    private var editing: CommentModel? = null

    private var authorName: String? = null
    private var authorPicture: String? = null

    private var postListener: ValueEventListener? = null

    private val myUid: String? get() = FBRef.auth.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BoardPostActivityBinding.inflate(layoutInflater)
        setContentViewEdgeToEdge(binding.root)

        id = intent.getStringExtra("ID") ?: ""
        // 조회수는 글 화면이 열릴 때 한 번만 올린다 (목록, 검색, 내 글 어디서 열어도 같다). 화면 회전 같은 재생성은 세지 않는다.
        if (savedInstanceState == null && id.isNotEmpty()) {
            FBRef.postRef.child(id).child("views").setValue(ServerValue.increment(1))
        }

        binding.boardPostRvComment.adapter = listAdapter
        binding.boardPostRvComment.layoutManager = LinearLayoutManager(this)
        listAdapter.listener = commentListener

        binding.boardPostIbBack.setOnClickListener { closeWithResult() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = closeWithResult()
        })
        binding.boardPostIbMenu.setOnClickListener { showPostMenu() }
        binding.boardPostBtnSend.setOnClickListener { sendComment() }
        binding.boardPostEditCancel.setOnClickListener { stopEditing() }

        BlockedUsers.blocked.observe(this) { renderComments() }
        boardViewModel.commentList.observe(this) { list ->
            listAdapter.submitList(list.toList())
            binding.boardPostCommentEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            updateStat(list.size)
        }

        loadPost()
    }

    override fun onDestroy() {
        postListener?.let { FBRef.postRef.child(id).removeEventListener(it) }
        super.onDestroy()
    }

    // ───────── 글 불러오기 ─────────

    private fun loadPost() {
        if (id.isEmpty()) {
            finish()
            return
        }
        setLoading(true)
        postListener = FBRef.postRef.child(id).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing || isDestroyed) return
                if (!snapshot.exists()) {
                    // 삭제된 글: 열려 있던 화면을 닫는다
                    finish()
                    return
                }
                val post = snapshot.getValue(BoardModel::class.java) ?: return
                board = post
                showPost(post)
                allComments = post.comments.values.sortedBy { it.date }
                renderComments()
                setLoading(false)
            }

            override fun onCancelled(error: DatabaseError) {
                setLoading(false)
            }
        })
    }

    private fun showPost(post: BoardModel) = with(binding) {
        val notice = post.category == "공지"
        boardPostTvCategory.text = post.category
        boardPostTvCategory.visibility = if (post.category.isBlank()) View.GONE else View.VISIBLE
        boardPostTvCategory.setBackgroundResource(if (notice) R.drawable.shape_post_tag_notice else R.drawable.shape_post_tag)
        boardPostTvCategory.setTextColor(ContextCompat.getColor(this@BoardPostActivity, if (notice) R.color.black else R.color.brand_mint))
        boardPostTvTitle.text = post.title
        boardPostTvContent.text = post.content
        boardPostTvDate.text = SimpleDateFormat("M.d HH:mm", Locale.KOREA).format(Date(post.date))
        boardPostIbMenu.visibility = View.VISIBLE
        updateStat(boardViewModel.commentList.value?.size ?: 0)

        AuthorCache.get(post.author) { author ->
            if (isFinishing || isDestroyed) return@get
            authorName = if (author.exists) author.name else getString(R.string.board_withdrawn_user)
            authorPicture = author.profilePicture
            boardPostTvUser.text = authorName
            if (author.exists) ProfileImage.load(boardPostIbProfile, author.profilePicture)
        }
        boardPostIbProfile.setOnClickListener {
            if (post.author != null && post.author != myUid) askChat(post.author, authorName, authorPicture)
        }
    }

    private fun updateStat(commentCount: Int) {
        val views = board?.views ?: 0
        binding.boardPostTvStat.text =
            "${getString(R.string.post_comment_count, commentCount)}   ·   ${getString(R.string.post_view_count, views)}"
    }

    private fun setLoading(loading: Boolean) = with(binding) {
        boardPostProgressbar.visibility = if (loading) View.VISIBLE else View.INVISIBLE
        nestedScrollView.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        boardPostCommentLayout.visibility = if (loading) View.INVISIBLE else View.VISIBLE
    }

    private fun renderComments() {
        val visible = allComments.filterNot {
            it.report.values.any { r -> r.uid == myUid } || BlockedUsers.isBlocked(it.author)
        }
        boardViewModel.initComment(visible.toMutableList())
    }

    // ───────── 댓글 ─────────

    private val commentListener = object : BoardCommentRecyclerViewAdpater.OnItemClickListener {
        override fun onMenuClick(comment: CommentModel, mine: Boolean, authorName: String?) {
            if (mine) {
                ActionSheet.show(
                    this@BoardPostActivity, listOf(
                        ActionSheet.Item(getString(R.string.post_menu_edit)) { startEditing(comment) },
                        ActionSheet.Item(getString(R.string.post_menu_delete), danger = true) { confirmDeleteComment(comment) }
                    )
                )
            } else {
                ActionSheet.show(
                    this@BoardPostActivity, listOf(
                        ActionSheet.Item(getString(R.string.menu_report)) { reportComment(comment) },
                        ActionSheet.Item(getString(R.string.menu_block_user), danger = true) {
                            BlockDialog.show(this@BoardPostActivity, comment.author, authorName)
                        }
                    )
                )
            }
        }

        override fun onProfileClick(comment: CommentModel, authorName: String?, profilePicture: String?) {
            askChat(comment.author, authorName, profilePicture)
        }
    }

    private fun sendComment() {
        val content = binding.boardPostEtComment.text.toString().trim()
        if (content.isEmpty()) return
        val target = editing
        val task = if (target != null) {
            FBRef.postRef.child(id).child("comments").child(target.id).child("content").setValue(content)
        } else {
            val key = FBRef.postRef.child(id).child("comments").push().key ?: return
            val comment = CommentModel(key, BoardSingletone.LoginUser().uid, content, System.currentTimeMillis())
            FBRef.postRef.child(id).child("comments").child(key).setValue(comment)
        }
        task.addOnFailureListener {
            Toast.makeText(applicationContext, R.string.post_comment_failed, Toast.LENGTH_SHORT).show()
        }
        binding.boardPostEtComment.setText("")
        stopEditing()
        hideKeyboard()
        if (target == null) {
            // 새 댓글이면 맨 아래로 내려 보여 준다
            binding.nestedScrollView.post { binding.nestedScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun startEditing(comment: CommentModel) = with(binding) {
        editing = comment
        boardPostEditBanner.visibility = View.VISIBLE
        boardPostEtComment.setText(comment.content)
        boardPostEtComment.setSelection(boardPostEtComment.text.length)
        boardPostEtComment.requestFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(boardPostEtComment, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun stopEditing() = with(binding) {
        if (editing != null) boardPostEtComment.setText("")
        editing = null
        boardPostEditBanner.visibility = View.GONE
    }

    private fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.boardPostEtComment.windowToken, 0)
    }

    private fun confirmDeleteComment(comment: CommentModel) {
        AppDialog.confirm(
            this,
            title = getString(R.string.post_comment_delete_confirm),
            confirmText = getString(R.string.post_menu_delete),
            danger = true
        ) {
            if (editing?.id == comment.id) stopEditing()
            FBRef.postRef.child(id).child("comments").child(comment.id).removeValue()
        }
    }

    private fun reportComment(comment: CommentModel) {
        ReportCommentDialog(this, object : DialogListener {
            override fun onOKButtonClicked() {
                val uid = myUid ?: return
                val ref = FBRef.postRef.child(id).child("comments").child(comment.id).child("report")
                val key = ref.push().key ?: return
                ref.child(key).setValue(ReportModel(uid, System.currentTimeMillis()))
            }
        }).show()
    }

    // ───────── 글 메뉴 ─────────

    private fun showPostMenu() {
        val post = board ?: return
        if (post.author == myUid) {
            ActionSheet.show(
                this, listOf(
                    ActionSheet.Item(getString(R.string.post_menu_edit)) {
                        startActivity(Intent(this, BoardUpdateActivity::class.java).putExtra("updateBoard", post))
                    },
                    ActionSheet.Item(getString(R.string.post_menu_delete), danger = true) { confirmDeletePost(post) }
                )
            )
        } else {
            ActionSheet.show(
                this, listOf(
                    ActionSheet.Item(getString(R.string.menu_report)) { showReportBoardDialog() },
                    ActionSheet.Item(getString(R.string.menu_report_author)) {
                        UserReportDialog.show(this, post.author, "post", id)
                    },
                    ActionSheet.Item(getString(R.string.menu_block_author), danger = true) {
                        BlockDialog.show(this, post.author, authorName) { finishAsRemoved() }
                    }
                )
            )
        }
    }

    private fun confirmDeletePost(post: BoardModel) {
        AppDialog.confirm(
            this,
            title = getString(R.string.post_delete_confirm),
            confirmText = getString(R.string.post_menu_delete),
            danger = true
        ) {
            FBRef.postRef.child(post.id).removeValue()
            finish()
        }
    }

    private fun showReportBoardDialog() {
        ReportBoardDialog(this, object : DialogListener {
            override fun onOKButtonClicked() {
                val uid = myUid ?: return
                FBRef.postRef.child(id).child("report").push().setValue(ReportModel(uid, System.currentTimeMillis()))
                finishAsRemoved()
            }
        }).show()
    }

    private fun askChat(uid: String?, name: String?, picture: String?) {
        if (uid.isNullOrEmpty()) return
        BoardDialog(this, name ?: "", object : DialogListener {
            override fun onOKButtonClicked() {
                startActivity(ChatActivity.newIntent(this@BoardPostActivity, ERModel(uid = uid, profilePicture = picture, name = name)))
            }
        }).show()
    }

    // ───────── 닫기 ─────────

    private fun closeWithResult() {
        intent.putExtra("updateBoard", board)
        setResult(RESULT_OK, intent)
        finish()
    }

    // 신고하거나 작성자를 차단한 글은 목록에서 빼도록 알리고 닫는다
    private fun finishAsRemoved() {
        intent.putExtra("updateBoard", board?.copy(category = "report"))
        setResult(RESULT_OK, intent)
        finish()
    }
}
