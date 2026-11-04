package com.irionna.eternalreturninfo.presentation.activity.board

import androidx.appcompat.app.AppCompatActivity
import com.irionna.eternalreturninfo.util.setContentViewEdgeToEdge
import android.os.Bundle
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.BoardAddActivityBinding
import com.irionna.eternalreturninfo.data.model.BoardModel
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.retrofit.FBRef

class BoardUpdateActivity : AppCompatActivity() {

    private lateinit var binding: BoardAddActivityBinding
    private var category = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BoardAddActivityBinding.inflate(layoutInflater)
        setContentViewEdgeToEdge(binding.root)

        initView()
    }

    private fun initView() = with(binding) {

        val board = intent.getParcelableExtra<BoardModel>("updateBoard")

        if (board != null) {
            boardAddEtTitle.setText(board.title)
            boardAddEtContent.setText(board.content)

            when(board.category){
                "공지" -> {
                    boardAddSpinner.selectItemByIndex(0)
                    category = "공지"
                }
                "일상" -> {
                    boardAddSpinner.selectItemByIndex(1)
                    category = "일상"
                }
                "질문" -> {
                    boardAddSpinner.selectItemByIndex(2)
                    category = "질문"
                }
                "공략" -> {
                    boardAddSpinner.selectItemByIndex(3)
                    category = "공략"
                }
            }
        }

        if(BoardSingletone.LoginUser().uid != BoardSingletone.manager().uid){
            val spinnerItems = resources.getStringArray(R.array.board_user_option)
            boardAddSpinner.setItems(spinnerItems.toList())
        }else{
            val spinnerItems = resources.getStringArray(R.array.board_option)
            boardAddSpinner.setItems(spinnerItems.toList())
        }

        boardAddSpinner.setOnSpinnerItemSelectedListener<String> { oldIndex, oldItem, newIndex, newText ->

            when(newText){
                "공지" -> category = "공지"
                "일상" -> category = "일상"
                "질문" -> category = "질문"
                "공략" -> category = "공략"
            }
        }

        boardAddIbBack.setOnClickListener {
            finish()
        }

        boardAddBtnFinish.setOnClickListener {

            val category = category
            val title = boardAddEtTitle.text.toString()
            val content = boardAddEtContent.text.toString()

            // 보안 규칙상 글 수정은 제목, 내용, 분류만 필드 단위로 바꿀 수 있다.
            // (노드 전체를 덮어쓰면 댓글까지 다시 쓰게 되어 규칙에서 막힌다.)
            if (board != null) {
                FBRef.postRef.child(board.id).updateChildren(
                    mapOf(
                        "category" to category,
                        "title" to title,
                        "content" to content
                    )
                )
            }

            finish()

        }

    }

}