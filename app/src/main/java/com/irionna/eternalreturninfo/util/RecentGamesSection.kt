package com.irionna.eternalreturninfo.util

import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.repository.GameStats
import com.irionna.eternalreturninfo.databinding.RecentGameItemBinding
import com.irionna.eternalreturninfo.databinding.RecentGamesSectionBinding
import com.irionna.eternalreturninfo.retrofit.UserGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * 최근 경기 목록. 홈(간단히: 처음 몇 경기)과 프로필(자세히: 필터와 더 보기)이 같은 부품을 쓴다.
 *  - 순위 칸: 1등은 초록, 2등은 연한 초록, 3등은 회색, 나머지는 어두운 칸. 1등(우승)에는 초록 별(★).
 *  - 서버가 주는 실험체 번호는 게임 번호라서 [Characters] 로 앱 목록 위치로 바꿔 얼굴을 보여준다.
 * @param limit 간단히 모드에서 보여줄 경기 수 (자세히 모드에서는 무시)
 */
class RecentGamesSection(
    private val b: RecentGamesSectionBinding,
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val detailed: Boolean,
    private val limit: Int = 3
) {
    private enum class Filter { ALL, RANKED, NORMAL }

    private var userId: String? = null
    private val loaded = mutableListOf<UserGame>()
    private var next: Long? = null
    private var filter = Filter.ALL
    private var token = 0

    init {
        b.rgsMore.setOnClickListener { loadMore() }
        b.rgsFilter.visibility = View.GONE
    }

    /** 새 검색을 시작할 때: 이전 결과를 지우고 "불러오는 중"으로 돌린다 (늦게 도착하는 이전 응답은 무시한다). */
    fun reset() {
        token++
        loaded.clear()
        next = null
        showMessage(activity.getString(R.string.rg_loading))
    }

    /** 새 유저의 기록을 처음부터 불러온다. */
    fun load(userId: String) {
        this.userId = userId
        val t = ++token
        loaded.clear()
        next = null
        filter = Filter.ALL
        showMessage(activity.getString(R.string.rg_loading))
        scope.launch {
            val page = GameStats.games(userId)
            if (t != token) return@launch // 그 사이 다른 유저를 검색했다
            if (page == null) {
                showMessage(activity.getString(R.string.rg_failed))
                return@launch
            }
            loaded.addAll(page.games)
            next = page.next
            render()
        }
    }

    private fun loadMore() {
        val id = userId ?: return
        val cursor = next ?: return
        val t = token
        b.rgsMore.isEnabled = false
        scope.launch {
            val page = GameStats.games(id, cursor)
            if (t != token) return@launch
            b.rgsMore.isEnabled = true
            if (page != null) {
                loaded.addAll(page.games)
                next = page.next
                render()
            }
        }
    }

    private fun showMessage(text: String) {
        b.rgsEmpty.text = text
        b.rgsEmpty.visibility = View.VISIBLE
        b.rgsList.removeAllViews()
        b.rgsMore.visibility = View.GONE
        b.rgsFilter.visibility = View.GONE
        b.rgsSub.text = ""
    }

    private fun render() {
        val shown = loaded.filter {
            when (filter) {
                Filter.ALL -> true
                Filter.RANKED -> it.matchingMode == GameStats.MODE_RANKED
                Filter.NORMAL -> it.matchingMode == GameStats.MODE_NORMAL
            }
        }.let { if (detailed) it else it.take(limit) }

        b.rgsFilter.visibility = if (detailed && loaded.isNotEmpty()) View.VISIBLE else View.GONE
        if (detailed) fillFilter()
        b.rgsSub.text = if (detailed) "" else activity.getString(R.string.rg_sub_count, shown.size)

        b.rgsList.removeAllViews()
        if (shown.isEmpty()) {
            b.rgsEmpty.text = activity.getString(R.string.rg_empty)
            b.rgsEmpty.visibility = View.VISIBLE
        } else {
            b.rgsEmpty.visibility = View.GONE
            shown.forEach { b.rgsList.addView(rowFor(it)) }
        }
        // 자세히 모드에서만 더 불러올 수 있다. 서버가 다음 쪽을 알려 준 경우에만 버튼을 보인다.
        b.rgsMore.visibility = if (detailed && next != null) View.VISIBLE else View.GONE
    }

    private fun rowFor(g: UserGame): View {
        val row = RecentGameItemBinding.inflate(LayoutInflater.from(activity), b.rgsList, false)
        val rank = g.gameRank ?: 0
        row.rgRank.text = if (rank > 0) rank.toString() else "-"
        when (rank) {
            1 -> { row.rgRank.setBackgroundResource(R.drawable.shape_rank_1); row.rgRank.setTextColor(ContextCompat.getColor(activity, R.color.black)) }
            2 -> { row.rgRank.setBackgroundResource(R.drawable.shape_rank_2); row.rgRank.setTextColor(ContextCompat.getColor(activity, R.color.brand_mint)) }
            3 -> { row.rgRank.setBackgroundResource(R.drawable.shape_rank_3); row.rgRank.setTextColor(ContextCompat.getColor(activity, R.color.white)) }
            else -> { row.rgRank.setBackgroundResource(R.drawable.shape_rank_n); row.rgRank.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary)) }
        }
        row.rgStar.visibility = if (rank == 1) View.VISIBLE else View.GONE

        val code = g.characterNum ?: 0
        val name = Characters.nameOf(activity, code)
        val appIndex = Characters.appIndexOf(code)
        if (appIndex != null) CharacterPicker.loadFace(row.rgFace, appIndex) else row.rgFace.setImageDrawable(InitialAvatar.drawable(activity, name))
        row.rgFace.contentDescription = name

        row.rgTitle.text = GameLabels.mode(activity, g.matchingMode, g.matchingTeamMode)
        row.rgKda.text = activity.getString(R.string.rg_kda, g.playerKill ?: 0, g.playerAssistant ?: 0)
        row.rgTime.text = GameLabels.relativeTime(activity, g.startDtm)

        if (detailed) {
            val minutes = (g.duration ?: 0) / 60
            val damage = g.damageToPlayer
            row.rgSub.text = if (damage != null && damage > 0) {
                activity.getString(R.string.rg_detail, name, NumberFormat.getInstance(Locale.KOREA).format(damage), minutes)
            } else activity.getString(R.string.rg_detail_no_damage, name, minutes)
            row.rgSub.visibility = View.VISIBLE
        }
        return row.root
    }

    private fun fillFilter() {
        val row = b.rgsFilter
        row.removeAllViews()
        val dp = activity.resources.displayMetrics.density
        listOf(
            Filter.ALL to R.string.rg_filter_all,
            Filter.RANKED to R.string.rg_filter_ranked,
            Filter.NORMAL to R.string.rg_filter_normal
        ).forEach { (f, label) ->
            row.addView(TextView(activity).apply {
                text = activity.getString(label)
                gravity = Gravity.CENTER
                textSize = 12f
                minHeight = (40 * dp).toInt()
                setPadding((14 * dp).toInt(), 0, (14 * dp).toInt(), 0)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                if (f == filter) {
                    setBackgroundResource(R.drawable.shape_seg_on)
                    setTextColor(ContextCompat.getColor(activity, R.color.brand_mint))
                    setTypeface(typeface, Typeface.BOLD)
                } else {
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    setOnClickListener { filter = f; render() }
                }
            })
        }
    }
}
