package com.irionna.eternalreturninfo.presentation.adapter.board

import com.irionna.eternalreturninfo.util.ProfileImage
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.databinding.BoardPostRvCommentItemBinding
import com.irionna.eternalreturninfo.data.model.CommentModel
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.retrofit.FBRef
import com.irionna.eternalreturninfo.presentation.activity.board.BoardDialog
import com.irionna.eternalreturninfo.presentation.activity.board.DialogListener
import com.irionna.eternalreturninfo.presentation.activity.chat.ChatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.skydoves.powermenu.CircularEffect
import com.skydoves.powermenu.MenuAnimation
import com.skydoves.powermenu.OnMenuItemClickListener
import com.skydoves.powermenu.PowerMenu
import com.skydoves.powermenu.PowerMenuItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BoardCommentRecyclerViewAdpater(
    context: Context
) : ListAdapter<CommentModel, BoardCommentRecyclerViewAdpater.ViewHolder>(

    object : DiffUtil.ItemCallback<CommentModel>() {
        override fun areItemsTheSame(
            oldItem: CommentModel,
            newItem: CommentModel
        ): Boolean {
            return oldItem.author == newItem.author
        }

        override fun areContentsTheSame(
            oldItem: CommentModel,
            newItem: CommentModel
        ): Boolean {
            return oldItem == newItem
        }
    }
) {

    interface OnItemClickListener {
        fun onDeleteItemClick(commentItem: CommentModel, position:Int)
        fun onUpdateItemClick(commentItem: CommentModel, position:Int)
        fun onReportItemClick(commentItem: CommentModel, position:Int)
    }

    private var onDeleteItemClickListener: OnItemClickListener? = null
    private var onUpdateItemClickListener: OnItemClickListener? = null
    private var onReportItemClickListener: OnItemClickListener? = null

    private val refPowerMenu: PowerMenu by lazy {
        PowerMenu.Builder(context)
            .addItem(PowerMenuItem("수정"))
            .addItem(PowerMenuItem("삭제"))
            .setMenuRadius(20f) // sets the corner radius.
            .setTextSize(18)
            .setWidth(330)
            .setTextGravity(Gravity.CENTER)
            .setTextColor(ContextCompat.getColor(context, R.color.white))
            .setMenuColor(ContextCompat.getColor(context, R.color.darkgray))
            .setSelectedMenuColor(ContextCompat.getColor(context, R.color.black))
            .setCircularEffect(CircularEffect.BODY)
            .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
            .build()
    }

    private val refPowerMenuForReport: PowerMenu by lazy {
        PowerMenu.Builder(context)
            .addItem(PowerMenuItem("신고"))
            .setMenuRadius(20f) // sets the corner radius.
            .setTextSize(18)
            .setWidth(330)
            .setTextGravity(Gravity.CENTER)
            .setTextColor(ContextCompat.getColor(context, R.color.white))
            .setMenuColor(ContextCompat.getColor(context, R.color.darkgray))
            .setSelectedMenuColor(ContextCompat.getColor(context, R.color.black))
            .setCircularEffect(CircularEffect.BODY)
            .setAnimation(MenuAnimation.SHOWUP_TOP_RIGHT)
            .build()
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onDeleteItemClickListener = listener
        this.onUpdateItemClickListener = listener
        this.onReportItemClickListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            BoardPostRvCommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            parent.context
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(
        private val binding: BoardPostRvCommentItemBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommentModel) = with(binding) {

            FBRef.userRef.child(item?.author.toString()).addValueEventListener(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if(snapshot.exists()){
//                        val author = snapshot.getValue<ERModel>()
                        val author = snapshot.getValue(ERModel::class.java)

                        boardCommentTvUser.text = author?.name

                        ProfileImage.load(boardCommentIbProfile, author?.profilePicture)

                        // 로그인한 사용자면 ibMenu 보여주기
                        if (author?.uid == FBRef.auth.uid) {
                            boardCommentIbMenu.visibility = View.VISIBLE
                            boardCommentIbMenuReport.visibility = View.GONE
                            boardCommentIbMenu.setOnClickListener {

                                // 팝업메뉴 onClick 리스너
                                val onMenuItemClickListener = object : OnMenuItemClickListener<PowerMenuItem> {
                                    override fun onItemClick(position: Int, item2: PowerMenuItem) {
                                        when (position) {
                                            // 0 : 수정,   1 : 삭제
                                            0 -> {
                                                refPowerMenu.dismiss()
                                                onUpdateItemClickListener?.onUpdateItemClick(item, adapterPosition)
                                            }
                                            else -> {
                                                refPowerMenu.dismiss()
                                                onDeleteItemClickListener?.onDeleteItemClick(item, adapterPosition)
                                            }
                                        }
                                    }
                                }

                                refPowerMenu.showAsDropDown(it)
                                refPowerMenu.setOnMenuItemClickListener(onMenuItemClickListener)
                            }
                        } else {
                            boardCommentIbMenuReport.visibility = View.VISIBLE
                            boardCommentIbMenu.visibility = View.GONE
                            boardCommentIbMenuReport.setOnClickListener {

                                val onMenuItemClickListener = object : OnMenuItemClickListener<PowerMenuItem> {
                                    override fun onItemClick(position: Int, item2: PowerMenuItem) {
                                        when (position) {
                                            // 0 : 신고
                                            0 -> {
                                                refPowerMenuForReport.dismiss()
                                                onReportItemClickListener?.onReportItemClick(item, adapterPosition)
                                            }
                                        }
                                    }
                                }

                                refPowerMenuForReport.showAsDropDown(it)
                                refPowerMenuForReport.setOnMenuItemClickListener(onMenuItemClickListener)

                            }

                            boardCommentIbProfile.setOnClickListener {
                                val customDialog = BoardDialog(binding.root.context, author?.name.toString(), object : DialogListener {
                                    override fun onOKButtonClicked() {
                                        val intent = ChatActivity.newIntent(binding.root.context,
                                            ERModel(
                                                uid = author?.uid,
                                                profilePicture = author?.profilePicture,
                                                name = author?.name
                                            )
                                        )
                                        binding.root.context.startActivity(intent)
                                    }
                                })

                                customDialog.show()
                            }
                        }
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                }

            })

            boardCommentTvContent.text = item.content
            boardCommentTvDate.text = formatTimeOrDate(item.date)
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

    }
}