package com.irionna.eternalreturninfo.presentation.adapter.board

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.BoardModel
import com.irionna.eternalreturninfo.databinding.BoardRvItemBinding
import com.irionna.eternalreturninfo.util.AuthorCache
import com.irionna.eternalreturninfo.util.GameLabels
import com.irionna.eternalreturninfo.util.ProfileImage

/**
 * 게시글 카드 목록 (게시판, 검색, 내 글이 함께 쓴다).
 * 작성자 이름·프로필은 [AuthorCache] 로 받는다. 스크롤 중에 카드가 다른 글로 재사용되면 늦게 온 응답은 무시한다.
 */
class BoardRecyclerViewAdapter() : ListAdapter<BoardModel, BoardRecyclerViewAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<BoardModel>() {
        override fun areItemsTheSame(oldItem: BoardModel, newItem: BoardModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: BoardModel, newItem: BoardModel) = oldItem == newItem
    }
) {

    interface OnItemClickListener {
        fun onItemClick(boardItem: BoardModel)
    }

    private var onItemClickListener: OnItemClickListener? = null

    // 검색 결과에서 강조할 단어 (비어 있으면 강조 없음)
    private var highlight: String = ""

    fun setHighlight(query: String) {
        if (highlight == query) return
        highlight = query
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(BoardRvItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: BoardRvItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BoardModel) = with(binding) {
            val ctx = root.context

            // 작성자: 저장된 값이 있으면 바로, 없으면 한 번 받아서 채운다
            boardPostTvUser.text = ""
            ProfileImage.load(boardPostIvProfile, ProfileImage.DEFAULT_KEY)
            boardPostIvProfile.tag = item.id
            AuthorCache.get(item.author) { author ->
                if (boardPostIvProfile.tag != item.id) return@get // 그 사이 카드가 다른 글로 바뀌었다
                if (author.exists) {
                    ProfileImage.load(boardPostIvProfile, author.profilePicture)
                    boardPostTvUser.text = author.name
                } else {
                    boardPostTvUser.text = ctx.getString(R.string.board_withdrawn_user)
                }
            }

            val notice = item.category == "공지"
            boardPostCategory.text = item.category
            boardPostCategory.setBackgroundResource(if (notice) R.drawable.shape_post_tag_notice else R.drawable.shape_post_tag)
            boardPostCategory.setTextColor(ContextCompat.getColor(ctx, if (notice) R.color.black else R.color.brand_mint))
            boardPostPin.visibility = if (notice) View.VISIBLE else View.GONE
            boardPostLayout.setBackgroundResource(if (notice) R.drawable.shape_card_notice else R.drawable.shape_surface_card)

            boardPostTime.text = if (item.date > 0) GameLabels.relativeTimeMillis(ctx, item.date) else ""
            boardPostTvTitle.text = highlighted(item.title, ctx)
            boardPostTvContent.text = highlighted(item.content, ctx)
            boardPostViews.text = ctx.getString(R.string.board_views, item.views)
            boardPostBtnComment.text = ctx.getString(R.string.board_comments_count, item.comments.size)

            boardPostLayout.setOnClickListener {
                onItemClickListener?.onItemClick(item)
            }
        }
    }

    private fun highlighted(text: String, ctx: android.content.Context): CharSequence {
        if (highlight.isEmpty()) return text
        val span = android.text.SpannableString(text)
        var from = 0
        while (true) {
            val at = text.indexOf(highlight, from, ignoreCase = true)
            if (at < 0) break
            val end = at + highlight.length
            span.setSpan(
                android.text.style.BackgroundColorSpan(ContextCompat.getColor(ctx, R.color.tab_pill)),
                at, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            span.setSpan(
                android.text.style.ForegroundColorSpan(ContextCompat.getColor(ctx, R.color.brand_mint)),
                at, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            from = end
        }
        return span
    }
}
