package com.irionna.eternalreturninfo.util

import android.app.Activity
import android.app.Dialog
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.repository.GameStats
import com.irionna.eternalreturninfo.databinding.SeasonSheetBinding
import com.irionna.eternalreturninfo.databinding.StatsPanelBinding
import com.irionna.eternalreturninfo.retrofit.UserStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 전적 카드(stats_panel.xml)를 채우고 조작을 처리한다. 홈과 프로필 화면이 함께 쓴다.
 *  - 시즌 칩을 누르면 시즌 목록 시트가 열리고, 현재 시즌이 기본으로 선택된다.
 *  - 랭크/일반, 솔로/듀오/스쿼드는 서버가 실제로 준 기록에 있는 조합만 선택할 수 있다.
 *  - [onStatsChanged] 는 선택된 기록이 바뀔 때마다 불린다 (없으면 null).
 * [scope] 는 화면 수명에 묶인 것을 넘긴다 (화면이 닫힌 뒤 늦은 응답이 화면을 건드리지 않도록).
 */
class StatsPanel(
    private val b: StatsPanelBinding,
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val onStatsChanged: (UserStats?) -> Unit = {}
) {
    private var userId: String? = null
    private var seasons: List<GameStats.SeasonInfo> = emptyList()
    private var seasonId: Int? = null
    private var stats: List<UserStats> = emptyList()
    private var selMode = GameStats.MODE_RANKED
    private var selTeam = 0

    init {
        b.statsSeasonChip.setOnClickListener { if (seasons.isNotEmpty()) showSeasonSheet() }
    }

    /** [nicknameProvider] 는 닉네임이 준비될 때까지 기다려 돌려주는 함수 (로그인 직후엔 비어 있을 수 있다). */
    fun load(nicknameProvider: suspend () -> String?) {
        showMessage(activity.getString(R.string.stats_loading))
        scope.launch {
            val nickname = nicknameProvider()
            val num = if (nickname.isNullOrBlank()) null else GameStats.userId(nickname)
            if (num == null) {
                showMessage(activity.getString(R.string.stats_unavailable))
                return@launch
            }
            userId = num
            seasons = GameStats.seasons()
            val current = GameStats.currentSeasonId(seasons)
            if (current == null) {
                showMessage(activity.getString(R.string.stats_unavailable))
                return@launch
            }
            loadSeason(current, firstLoad = true)
        }
    }

    private fun loadSeason(id: Int, firstLoad: Boolean = false) {
        val num = userId ?: return
        seasonId = id
        updateChip()
        showMessage(activity.getString(R.string.stats_loading))
        scope.launch {
            val list = GameStats.stats(num, id)
            if (seasonId != id) return@launch // 그 사이 다른 시즌을 골랐다
            if (list == null) {
                val denied = GameStats.lastStatsError.let { it == 401 || it == 403 }
                showMessage(activity.getString(if (denied) R.string.stats_denied else R.string.stats_unavailable))
                return@launch
            }
            stats = list
            val default = GameStats.pickDefault(list)
            if (default != null) {
                selMode = default.matchingMode
                selTeam = default.matchingTeamMode
            }
            render()
        }
    }

    private fun updateChip() {
        val season = seasons.firstOrNull { it.id == seasonId }
        val name = season?.name ?: activity.getString(R.string.stats_season_loading)
        b.statsSeasonChip.text = if (season?.isCurrent == true) {
            "$name · ${activity.getString(R.string.stats_season_current)}"
        } else name
    }

    private fun showMessage(text: String) {
        b.statsEmpty.text = text
        b.statsEmpty.visibility = View.VISIBLE
        b.statsBody.visibility = View.GONE
        b.statsModeRow.visibility = View.GONE
        b.statsTeamRow.visibility = View.GONE
        onStatsChanged(null)
    }

    // ---- 모드 선택 / 표시 ----

    private fun modeLabel(mode: Int) = when (mode) {
        GameStats.MODE_RANKED -> activity.getString(R.string.stats_mode_ranked)
        GameStats.MODE_NORMAL -> activity.getString(R.string.stats_mode_normal)
        else -> activity.getString(R.string.stats_mode_other, mode)
    }

    private fun teamLabel(team: Int) = when (team) {
        1 -> activity.getString(R.string.stats_team_solo)
        2 -> activity.getString(R.string.stats_team_duo)
        3 -> activity.getString(R.string.stats_team_squad)
        else -> activity.getString(R.string.stats_team_other, team)
    }

    private fun render() {
        if (stats.isEmpty()) {
            showMessage(activity.getString(R.string.stats_no_record))
            return
        }
        // 랭크 → 일반 → 그 밖의 순서로 보여준다
        val modes = stats.map { it.matchingMode }.distinct()
            .sortedBy { if (it == GameStats.MODE_RANKED) 0 else if (it == GameStats.MODE_NORMAL) 1 else 2 + it }
        if (selMode !in modes) selMode = modes.first()
        val teams = stats.filter { it.matchingMode == selMode }.map { it.matchingTeamMode }.distinct().sorted()
        if (selTeam !in teams) selTeam = teams.first()

        fillSegment(b.statsModeRow, modes.map { it to modeLabel(it) }, selMode) {
            selMode = it
            render()
        }
        fillSegment(b.statsTeamRow, teams.map { it to teamLabel(it) }, selTeam) {
            selTeam = it
            render()
        }

        val entry = stats.firstOrNull { it.matchingMode == selMode && it.matchingTeamMode == selTeam }
        b.statsModeRow.visibility = View.VISIBLE
        b.statsTeamRow.visibility = View.VISIBLE
        b.statsEmpty.visibility = View.GONE
        b.statsBody.visibility = View.VISIBLE
        if (entry != null) bind(entry)
        onStatsChanged(entry)
    }

    private fun fillSegment(row: LinearLayout, items: List<Pair<Int, String>>, selected: Int, onPick: (Int) -> Unit) {
        row.removeAllViews()
        val dp = activity.resources.displayMetrics.density
        items.forEach { (value, label) ->
            val tv = TextView(activity).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 13f
                setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                if (value == selected) {
                    setBackgroundResource(R.drawable.shape_seg_on)
                    setTextColor(ContextCompat.getColor(activity, R.color.brand_mint))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                } else {
                    setTextColor(ContextCompat.getColor(activity, R.color.text_secondary))
                    setOnClickListener { onPick(value) }
                }
            }
            row.addView(tv)
        }
    }

    private fun pct(rate: Float) = String.format("%.1f", rate * 100)

    private fun bind(s: UserStats) {
        val ranked = s.matchingMode == GameStats.MODE_RANKED
        b.statsRpBlock.visibility = if (ranked) View.VISIBLE else View.GONE
        b.statsRpSpacer.visibility = if (ranked) View.GONE else View.VISIBLE
        if (ranked) {
            b.statsRp.text = "${s.mmr} RP"
            b.statsRank.text = if (s.rank > 0) {
                activity.getString(R.string.stats_rank_line, s.rank, pct(s.rankPercent))
            } else activity.getString(R.string.stats_no_rank)
        }
        b.statsVGames.text = s.totalGames.toString()
        b.statsVWin.text = "${pct(s.top1)}%"
        b.statsVTop3.text = "${pct(s.top3)}%"
        b.statsVRank.text = "#" + String.format("%.1f", s.averageRank)
        b.statsVKill.text = String.format("%.1f", s.averageKills)
        b.statsVAssist.text = String.format("%.1f", s.averageAssistants)
        b.statsPbWin.progress = (s.top1 * 100).toInt().coerceIn(0, 100)
        b.statsPbTop3.progress = (s.top3 * 100).toInt().coerceIn(0, 100)

        // 가장 많이 한 실험체 3명 (캐릭터 코드 = 실험체 목록 번호)
        val names = activity.resources.getStringArray(R.array.characterName)
        val top = s.characterStats.orEmpty().sortedByDescending { it.usages }.take(3)
        val views = listOf(
            Triple(b.statsC1, b.statsC1Img, Pair(b.statsC1Name, b.statsC1Count)),
            Triple(b.statsC2, b.statsC2Img, Pair(b.statsC2Name, b.statsC2Count)),
            Triple(b.statsC3, b.statsC3Img, Pair(b.statsC3Name, b.statsC3Count))
        )
        views.forEachIndexed { i, (box, img, texts) ->
            val c = top.getOrNull(i)
            if (c == null) {
                box.visibility = View.INVISIBLE
            } else {
                box.visibility = View.VISIBLE
                CharacterPicker.loadFace(img, c.characterCode)
                texts.first.text = names.getOrNull(c.characterCode) ?: "#${c.characterCode}"
                texts.second.text = activity.getString(R.string.stats_games_unit, c.usages.toInt())
            }
        }
    }

    // ---- 시즌 시트 ----

    private fun showSeasonSheet() {
        val sheet = SeasonSheetBinding.inflate(LayoutInflater.from(activity))
        val dialog = Dialog(activity, R.style.ConsentSheetDialog)
        dialog.setContentView(sheet.root)
        val height = (activity.resources.displayMetrics.heightPixels * 0.55f).toInt()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height)
            setGravity(Gravity.BOTTOM)
        }
        val dp = activity.resources.displayMetrics.density
        seasons.forEach { season ->
            val on = season.id == seasonId
            val tv = TextView(activity).apply {
                text = if (season.isCurrent) "${season.name} (${activity.getString(R.string.stats_season_current)})" else season.name
                textSize = 15f
                setPadding((4 * dp).toInt(), (14 * dp).toInt(), (4 * dp).toInt(), (14 * dp).toInt())
                setTextColor(ContextCompat.getColor(activity, if (on) R.color.brand_mint else R.color.white))
                if (on) setTypeface(typeface, android.graphics.Typeface.BOLD)
                setOnClickListener {
                    dialog.dismiss()
                    if (season.id != seasonId) loadSeason(season.id)
                }
            }
            sheet.seasonList.addView(tv)
        }
        dialog.show()
    }

    companion object {
        /** 로그인 직후에는 닉네임이 비어 있을 수 있어서, 준비될 때까지 최대 [timeoutMs] 기다린다. */
        suspend fun waitForNickname(timeoutMs: Long = 15000, read: () -> String?): String? {
            var waited = 0L
            while (waited <= timeoutMs) {
                val n = read()
                if (!n.isNullOrBlank()) return n
                delay(500)
                waited += 500
            }
            return null
        }
    }
}
