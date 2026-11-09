package com.irionna.eternalreturninfo.presentation.activity.board

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.BoardModel
import com.irionna.eternalreturninfo.databinding.BoardAddActivityBinding
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.retrofit.FBRef
import com.irionna.eternalreturninfo.util.setContentViewEdgeToEdge

/**
 * 글쓰기 화면. 글 수정 화면([BoardUpdateActivity])도 같은 화면을 쓰며, "updateBoard" 로 글이 넘어오면 수정 모드가 된다.
 * 분류·제목·내용이 모두 채워져야 등록 버튼이 켜지고, 저장이 끝난 뒤에 화면을 닫는다.
 */
open class BoardAddActivity : AppCompatActivity() {

    private lateinit var binding: BoardAddActivityBinding
    private var category: String? = null
    private var saving = false
    private var editing: BoardModel? = null
    private var originalCategory = ""
    private var originalTitle = ""
    private var originalContent = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BoardAddActivityBinding.inflate(layoutInflater)
        setContentViewEdgeToEdge(binding.root)

        editing = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("updateBoard", BoardModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("updateBoard")
        }

        val isManager = BoardSingletone.LoginUser().uid == BoardSingletone.manager().uid
        val options = resources.getStringArray(if (isManager) R.array.board_option else R.array.board_user_option)
            .toMutableList()

        editing?.let { post ->
            binding.boardAddTvTitle.setText(R.string.write_title_edit)
            binding.boardAddBtnFinish.setText(R.string.write_save)
            // 목록에 없는 분류(예: 공지)의 글이어도 분류가 사라지지 않게 한다
            if (post.category.isNotBlank() && post.category !in options) options.add(0, post.category)
            if (savedInstanceState == null) {
                binding.boardAddEtTitle.setText(post.title)
                binding.boardAddEtContent.setText(post.content)
                category = post.category.takeIf { it.isNotBlank() }
            }
            originalCategory = post.category
            originalTitle = post.title
            originalContent = post.content
        }
        savedInstanceState?.getString("category")?.let { category = it }

        buildChips(options)
        refreshState()

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = refreshState()
        }
        binding.boardAddEtTitle.addTextChangedListener(watcher)
        binding.boardAddEtContent.addTextChangedListener(watcher)

        binding.boardAddIbBack.setOnClickListener { confirmLeave() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmLeave()
        })
        binding.boardAddBtnFinish.setOnClickListener { save() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("category", category)
    }

    private fun buildChips(options: List<String>) {
        val row: LinearLayout = binding.boardAddChips
        row.removeAllViews()
        val dp = resources.displayMetrics.density
        options.forEach { name ->
            val on = name == category
            row.addView(TextView(this).apply {
                text = name
                textSize = 13f
                setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
                setBackgroundResource(if (on) R.drawable.shape_chip_active else R.drawable.shape_chip)
                setTextColor(ContextCompat.getColor(context, if (on) R.color.brand_mint else R.color.white))
                setOnClickListener {
                    category = name
                    buildChips(options)
                    refreshState()
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * dp).toInt() }
            })
        }
    }

    private fun ready(): Boolean =
        !saving && !category.isNullOrEmpty() &&
            binding.boardAddEtTitle.text.toString().trim().isNotEmpty() &&
            binding.boardAddEtContent.text.toString().trim().isNotEmpty()

    private fun refreshState() = with(binding) {
        val ok = ready()
        boardAddBtnFinish.isEnabled = ok
        boardAddBtnFinish.setBackgroundResource(if (ok) R.drawable.shape_register_on else R.drawable.shape_register_off)
        boardAddBtnFinish.setTextColor(
            if (ok) 0xFF04140D.toInt() else ContextCompat.getColor(this@BoardAddActivity, R.color.text_secondary)
        )
        boardAddTvCount.text = "${boardAddEtContent.text.length} / 2000"
    }

    private fun hasChanges(): Boolean = with(binding) {
        val title = boardAddEtTitle.text.toString()
        val content = boardAddEtContent.text.toString()
        if (editing != null) {
            title != originalTitle || content != originalContent || category != originalCategory
        } else {
            title.isNotBlank() || content.isNotBlank()
        }
    }

    private fun confirmLeave() {
        if (saving || !hasChanges()) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setMessage(R.string.write_discard_confirm)
            .setPositiveButton(R.string.write_discard_ok) { _, _ -> finish() }
            .setNegativeButton(R.string.block_cancel, null)
            .show()
    }

    private fun save() {
        if (!ready()) return
        val title = binding.boardAddEtTitle.text.toString().trim()
        val content = binding.boardAddEtContent.text.toString().trim()
        val cat = category ?: return
        saving = true
        refreshState()

        val post = editing
        val task = if (post != null) {
            // 보안 규칙상 글 수정은 제목, 내용, 분류만 필드 단위로 바꿀 수 있다 (노드 전체를 덮어쓰면 댓글까지 다시 쓰게 되어 규칙에서 막힌다)
            FBRef.postRef.child(post.id).updateChildren(
                mapOf("category" to cat, "title" to title, "content" to content)
            )
        } else {
            val key = FBRef.postRef.push().key ?: run {
                saving = false
                refreshState()
                return
            }
            val newBoard = BoardModel(key, cat, title, content, BoardSingletone.LoginUser().uid, System.currentTimeMillis(), mapOf(), 0)
            intent.putExtra("board", newBoard)
            FBRef.postRef.child(key).setValue(newBoard)
        }
        task.addOnSuccessListener {
            setResult(RESULT_OK, intent)
            finish()
        }.addOnFailureListener {
            saving = false
            refreshState()
            Toast.makeText(applicationContext, R.string.write_failed, Toast.LENGTH_LONG).show()
        }
    }
}
