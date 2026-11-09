package com.irionna.eternalreturninfo.presentation.adapter.board

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.CommentModel
import com.irionna.eternalreturninfo.databinding.BoardPostRvCommentItemBinding
import com.irionna.eternalreturninfo.util.AuthorCache
import com.irionna.eternalreturninfo.util.GameLabels
import com.irionna.eternalreturninfo.util.ProfileImage

/**
 * 댓글 목록. 작성자 이름·프로필은 [AuthorCache] 로 받는다 (댓글마다 서버 연결을 열어 두지 않는다).
 * 본인 댓글이면 수정·삭제, 남의 댓글이면 신고·차단 메뉴를 [listener] 로 알린다.
 */
class BoardCommentRecyclerViewAdpater(
    private val postAuthorUid: () -> String?
) : ListAdapter<CommentModel, BoardCommentRecyclerViewAdpater.ViewHolder>(
    object : DiffUtil.ItemCallback<CommentModel>() {
        override fun areItemsTheSame(oldItem: CommentModel, newItem: CommentModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CommentModel, newItem: CommentModel) = oldItem == newItem
    }
) {

    interface OnItemClickListener {
        /** 댓글 ⋮ 를 눌렀다. [mine] 이면 내 댓글. [authorName] 은 차단 확인창에 쓴다. */
        fun onMenuClick(comment: CommentModel, mine: Boolean, authorName: String?)
        /** 남의 댓글 작성자 프로필을 눌렀다 (채팅 걸기). */
        fun onProfileClick(comment: CommentModel, authorName: String?, profilePicture: String?)
    }

    var listener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        BoardPostRvCommentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: BoardPostRvCommentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommentModel) = with(binding) {
            val ctx = root.context
            val mine = item.author == FirebaseAuth.getInstance().uid
            boardCommentTvContent.text = item.content
            boardCommentTvDate.text = GameLabels.relativeTimeMillis(ctx, item.date)
            boardCommentTvBadge.visibility =
                if (item.author != null && item.author == postAuthorUid()) View.VISIBLE else View.GONE

            // 이름은 받아 오기 전까지 비워 두고, 그 사이 카드가 다른 댓글로 재사용되면 늦게 온 응답은 무시한다
            boardCommentTvUser.text = ""
            ProfileImage.load(boardCommentIbProfile, ProfileImage.DEFAULT_KEY)
            boardCommentIbProfile.tag = item.id
            var name: String? = null
            var picture: String? = null
            AuthorCache.get(item.author) { author ->
                if (boardCommentIbProfile.tag != item.id) return@get
                name = if (author.exists) author.name else ctx.getString(R.string.board_withdrawn_user)
                picture = author.profilePicture
                boardCommentTvUser.text = name
                if (author.exists) ProfileImage.load(boardCommentIbProfile, author.profilePicture)
            }

            boardCommentIbMenu.setOnClickListener { listener?.onMenuClick(item, mine, name) }
            boardCommentIbProfile.setOnClickListener {
                if (!mine) listener?.onProfileClick(item, name, picture)
            }
        }
    }
}
