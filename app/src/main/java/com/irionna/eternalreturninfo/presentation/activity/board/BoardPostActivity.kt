package com.irionna.eternalreturninfo.presentation.activity.board

import com.irionna.eternalreturninfo.util.ProfileImage
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import coil.api.load
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.BoardPostActivityBinding
import com.irionna.eternalreturninfo.data.model.BoardModel
import com.irionna.eternalreturninfo.data.model.CommentModel
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.retrofit.FBRef
import com.irionna.eternalreturninfo.presentation.activity.chat.ChatActivity
import com.irionna.eternalreturninfo.presentation.adapter.board.BoardCommentRecyclerViewAdpater
import com.irionna.eternalreturninfo.presentation.viewmodel.BoardListViewModel
import com.irionna.eternalreturninfo.presentation.viewmodel.BoardListViewModelFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.irionna.eternalreturninfo.data.model.ReportModel
import com.skydoves.powermenu.CircularEffect
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.OnMenuItemClickListener
import com.skydoves.powermenu.PowerMenu
import com.skydoves.powermenu.PowerMenuItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BoardPostActivity : AppCompatActivity() {

    private lateinit var binding: BoardPostActivityBinding

    private val listAdapter by lazy {
        BoardCommentRecyclerViewAdpater(
            this
        )
    }

    private val boardViewModel by lazy {
        ViewModelProvider(this, BoardListViewModelFactory()).get(BoardListViewModel::class.java)
    }

    private var board: BoardModel? = null
    private var id: String = ""
    private var set: MutableSet<CommentModel> = mutableSetOf()

    private var dataload = false

    private val refPowerMenu: PowerMenu by lazy {
        PowerMenu.Builder(this@BoardPostActivity)
            .addItem(PowerMenuItem("수정"))
            .addItem(PowerMenuItem("삭제"))
            .setMenuRadius(20f) // sets the corner radius.
            .setTextSize(18)
            .setWidth(330)
            .setTextGravity(Gravity.CENTER)
            .setTextColor(ContextCompat.getColor(this@BoardPostActivity, R.color.white))
            .setMenuColor(ContextCompat.getColor(this@BoardPostActivity, R.color.darkgray))
            .setSelectedMenuColor(ContextCompat.getColor(this@BoardPostActivity, R.color.black))
            .setOnMenuItemClickListener(onMenuItemClickListener)
            .setCircularEffect(CircularEffect.BODY)
            .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
            .build()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BoardPostActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDataload()
        initView()
        initModel()

    }

    private fun progressbar(isLoading: Boolean) {

        if (isLoading) {
            binding.boardPostProgressbar.visibility = View.VISIBLE
            binding.boardPostCommentLayout.visibility = View.INVISIBLE
            binding.nestedScrollView.visibility = View.INVISIBLE
        } else {
            binding.boardPostProgressbar.visibility = View.GONE
            binding.boardPostCommentLayout.visibility = View.VISIBLE
            binding.nestedScrollView.visibility = View.VISIBLE
        }

    }

    private fun initDataload() = with(binding) {


        boardPostRvComment.adapter = listAdapter
        boardPostRvComment.layoutManager = LinearLayoutManager(this@BoardPostActivity)

        id = intent.getStringExtra("ID") ?: ""

        FBRef.postRef.child(id).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                progressbar(isLoading = true)

                if (snapshot.exists()) {
//                    board = snapshot.getValue<BoardModel>()
                    board = snapshot.getValue(BoardModel::class.java)

                    boardPostTvContent.text = board?.content
//                    boardPostTvVisit.text = board?.views.toString()

                    if (board?.category == "공지") {
                        boardPostTvTitle.text = board?.title
                        val blueColor = ContextCompat.getColor(binding.root.context, R.color.blue)
                        boardPostTvTitle.setTextColor(blueColor)
                    } else {
                        boardPostTvTitle.text = board?.title
                        boardPostTvTitle.setTextColor(Color.WHITE)
                    }

                    FBRef.userRef.child(board?.author.toString())
                        .addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {

                                if (snapshot.exists()) {
//                                val user = snapshot.getValue<ERModel>()
                                    val user = snapshot.getValue(ERModel::class.java)
                                    boardPostTvUser.text = user?.name

                                    ProfileImage.load(boardPostIbProfile, user?.profilePicture)

                                    // 게시글 작성자일 경우, 수정 삭제 활성화
                                    if (user?.uid == BoardSingletone.LoginUser().uid) {
                                        boardPostIbMenu.visibility = View.VISIBLE
                                        boardReportMenu.visibility = View.INVISIBLE

                                        boardPostIbMenu.setOnClickListener {
                                            refPowerMenu.showAsDropDown(it)
                                        }
                                    } else {
                                        boardPostIbMenu.visibility = View.INVISIBLE
                                        boardReportMenu.visibility = View.VISIBLE

                                        boardPostIbProfile.setOnClickListener {
                                            val customDialog = BoardDialog(
                                                this@BoardPostActivity,
                                                user?.name ?: "",
                                                object : DialogListener {
                                                    override fun onOKButtonClicked() {
                                                        startActivity(
                                                            ChatActivity.newIntent(
                                                                this@BoardPostActivity,
                                                                ERModel(
                                                                    uid = user?.uid,
                                                                    profilePicture = user?.profilePicture,
                                                                    name = user?.name
                                                                )
                                                            )
                                                        )
                                                    }
                                                })
                                            customDialog.show()
                                        }

                                        boardReportMenu.setOnClickListener{
                                            val reportBoardDialog = ReportBoardDialog(
                                                this@BoardPostActivity,
                                                object : DialogListener {
                                                    override fun onOKButtonClicked() {
                                                        val reportMap = ReportModel(FBRef.auth.uid!!, System.currentTimeMillis())
                                                        FBRef.postRef.child(id).child("report").push().setValue(reportMap)
                                                        intent.putExtra("updateBoard", board?.copy(category = "report"))
                                                        setResult(RESULT_OK, intent)
                                                        finish()
                                                    }
                                                }
                                            )
                                            reportBoardDialog.show()
                                        }
                                    }
                                }

                            }

                            override fun onCancelled(error: DatabaseError) {
                            }

                        })

                    if (board != null) {
                        boardPostTvDate.text =
                            board?.date?.let { formatTimeOrDate(it) }
                    }

                    if (board?.comments?.size == 0) {
                        boardPostBtnComment.visibility = View.INVISIBLE
                    } else {
                        boardPostBtnComment.text = board?.comments?.size.toString()

                        val comments = board?.comments?.values?.toList()
                        val sortedComment = comments?.sortedBy { Date(it.date) }


                        if (sortedComment != null) {

                            val filter = sortedComment.filterNot{
                                it.report.values.any { r -> r.uid == FBRef.auth.uid }
                            }


                            boardViewModel.initComment(filter.toMutableList())

                        }


                    }
                }

                progressbar(isLoading = false)
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })

        ProfileImage.load(boardPostIbCommentProfile, BoardSingletone.LoginUser().profilePicture)
    }

    private fun initView() = with(binding) {

        listAdapter.setOnItemClickListener(object :
            BoardCommentRecyclerViewAdpater.OnItemClickListener {
            override fun onDeleteItemClick(commentItem: CommentModel, position: Int) {
                FBRef.postRef.child(id).child("comments").child(commentItem.id).removeValue()
                boardViewModel.removeComment(position)
            }

            override fun onUpdateItemClick(commentItem: CommentModel, position: Int) {
                boardPostEtComment.setText(commentItem.content)
                boardPostBtnSave.visibility = View.INVISIBLE
                boardPostBtnUpdate.visibility = View.VISIBLE

                boardPostBtnUpdate.setOnClickListener {

                    val content = boardPostEtComment.text.toString()
                    val newComment = CommentModel(
                        commentItem.id,
                        commentItem.author,
                        content,
                        Calendar.getInstance().timeInMillis
                    )

                    FBRef.postRef.child(id).child("comments").child(commentItem.id)
                        .setValue(newComment)

                    boardPostEtComment.setText("")

                    boardPostBtnUpdate.visibility = View.INVISIBLE
                    boardPostBtnSave.visibility = View.VISIBLE
                }

            }

            override fun onReportItemClick(commentItem: CommentModel, position: Int) {

                val reportCommentDialog = ReportCommentDialog(
                    this@BoardPostActivity,
                    object : DialogListener {
                        override fun onOKButtonClicked() {
                            // 댓글 신고 눌렀을 시 해야할 작업들
                            // 1. 신고한 댓글 뷰모델에서 삭제
                            boardViewModel.removeComment(position)

                            // 2. 서버에 댓글 신고 등록
                            val commentKey = FBRef.postRef.child(id).child("comments").child(commentItem.id).child("report").push().key.toString()
                            val reportMap = ReportModel(FBRef.auth.uid!!, System.currentTimeMillis())

                            FBRef.postRef.child(id).child("comments").child(commentItem.id).child("report").child(commentKey).setValue(reportMap)
                        }
                    }
                )
                reportCommentDialog.show()

            }
        })

        boardPostBtnSave.setOnClickListener {
            val content = boardPostEtComment.text.toString()
            val date = Calendar.getInstance().timeInMillis

            val commentkey = FBRef.postRef.child(id).child("comments").push().key.toString()

            val newComment =
                CommentModel(commentkey, BoardSingletone.LoginUser().uid, content, date)

            FBRef.postRef.child(id).child("comments").child(commentkey).setValue(newComment)

            boardPostEtComment.setText("")
        }

        boardPostIbBack.setOnClickListener {
            intent.putExtra("updateBoard", board)
            setResult(RESULT_OK, intent)
            finish()
        }

        dataload = true

    }

    private fun initModel() = with(boardViewModel) {
        commentList.observe(this@BoardPostActivity) { commentList ->
            listAdapter.submitList(commentList)
            binding.boardPostBtnComment.text = commentList.size.toString()
        }
    }

    fun formatTimeOrDate(postTime: Long): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val date1 = calendar.time

        val simpleDateFormat: SimpleDateFormat
        if (Date(postTime) > date1) {
            simpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        } else {
            simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        }

        return simpleDateFormat.format(Date(postTime))
    }

    // 팝업메뉴 onClick 리스너
    private val onMenuItemClickListener = object :
        OnMenuItemClickListener<PowerMenuItem> {
        override fun onItemClick(position: Int, item: PowerMenuItem) {
            when (position) {
                // 0 : 수정,   1 : 삭제
                0 -> {
                    refPowerMenu.dismiss()
                    val updateIntent =
                        Intent(this@BoardPostActivity, BoardUpdateActivity::class.java)
                    updateIntent.putExtra("updateBoard", board)
                    startActivity(updateIntent)
                }

                else -> {
                    refPowerMenu.dismiss()
                    finish()
                    Log.d("board.id", board?.id.toString())
                    FBRef.postRef.child(board?.id.toString()).removeValue()
                }
            }
        }
    }

}