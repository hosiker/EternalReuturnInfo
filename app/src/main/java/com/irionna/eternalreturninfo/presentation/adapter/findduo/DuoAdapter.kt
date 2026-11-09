package com.irionna.eternalreturninfo.presentation.adapter.findduo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.ERModel
import com.irionna.eternalreturninfo.databinding.DuoCardItemBinding
import com.irionna.eternalreturninfo.util.CharacterPicker
import com.irionna.eternalreturninfo.util.GameLabels
import com.irionna.eternalreturninfo.util.InitialAvatar
import com.irionna.eternalreturninfo.util.RankDots
import com.irionna.eternalreturninfo.util.Tiers

/**
 * 듀오 모집글 카드 목록. 카드는 모집글에 저장된 값(티어, 승률, 최근 5경기)만 보여주고 전적 서버에는 묻지 않는다.
 * 내 모집글은 초록 테두리 + "모집글 내리기", 남의 모집글은 "듀오 신청 채팅"과 ⋯(신고·차단) 메뉴가 있다.
 */
class DuoAdapter(
    private val myUid: () -> String?,
    private val onChat: (ERModel) -> Unit,
    private val onRemoveMine: (ERModel) -> Unit,
    private val onMore: (ERModel, View) -> Unit
) : ListAdapter<ERModel, DuoAdapter.Holder>(object : DiffUtil.ItemCallback<ERModel>() {
    override fun areItemsTheSame(a: ERModel, b: ERModel) = a.uid == b.uid
    override fun areContentsTheSame(a: ERModel, b: ERModel) = a == b
}) {

    class Holder(val b: DuoCardItemBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(DuoCardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val b = holder.b
        val ctx = b.root.context
        val mine = item.uid != null && item.uid == myUid()

        // 모스트 실험체 얼굴 (앱 목록 이름으로 찾고, 없으면 첫 글자 아바타)
        val names = ctx.resources.getStringArray(R.array.characterName)
        val mostIndex = names.indexOf(item.most)
        if (mostIndex >= 1) CharacterPicker.loadFace(b.duoFace, mostIndex)
        else b.duoFace.setImageDrawable(InitialAvatar.drawable(ctx, item.name ?: "?"))
        b.duoFace.contentDescription = item.most

        b.duoName.text = item.name
        b.duoBadge.visibility = if (mine) View.VISIBLE else View.GONE
        val posted = item.timestamp?.toLongOrNull()
        b.duoTime.text = if (posted != null) GameLabels.relativeTimeMillis(ctx, posted) else ""

        b.duoTagServer.text = item.server
        b.duoTagTier.text = Tiers.displayName(item.tier)
        b.duoTagGender.text = item.gender

        // 전적 요약(등록할 때 저장한 값). 직접 입력한 티어는 요약이 없다.
        val recent = RankDots.parse(item.recent)
        val hasStats = !item.winrate.isNullOrBlank() && !item.avgrank.isNullOrBlank()
        if (hasStats) {
            b.duoStatsText.text = ctx.getString(R.string.duo_stats_line, item.winrate, item.avgrank) + "\n" + ctx.getString(R.string.duo_stats_recent)
        } else {
            b.duoStatsText.text = ctx.getString(R.string.duo_stats_none)
        }
        RankDots.fill(b.duoDots, recent)

        if (mine) {
            b.duoCard.setBackgroundResource(R.drawable.shape_card_mine)
            b.duoMore.visibility = View.GONE
            b.duoBtn.text = ctx.getString(R.string.duo_btn_remove)
            b.duoBtn.setBackgroundResource(R.drawable.shape_btn_outline)
            b.duoBtn.setTextColor(ContextCompat.getColor(ctx, R.color.brand_mint))
            b.duoBtn.setOnClickListener { onRemoveMine(item) }
        } else {
            b.duoCard.setBackgroundResource(R.drawable.shape_surface_card)
            b.duoMore.visibility = View.VISIBLE
            b.duoMore.setOnClickListener { onMore(item, it) }
            b.duoBtn.text = ctx.getString(R.string.duo_btn_chat)
            b.duoBtn.setBackgroundResource(R.drawable.shape_btn_primary)
            b.duoBtn.setTextColor(ContextCompat.getColor(ctx, R.color.black))
            b.duoBtn.setOnClickListener { onChat(item) }
        }
    }
}
