package com.irionna.eternalreturninfo.data.repository

import com.irionna.eternalreturninfo.util.Tiers

/**
 * 듀오 모집글에 같이 저장할 전적 요약. 모집글을 올릴 때 한 번 전적 서버에서 가져와 저장해 두고,
 * 목록의 카드는 이 저장값만 보여준다 (카드마다 전적 서버에 묻지 않아서 목록이 바로 뜬다).
 * 티어는 전적 서버의 랭크 점수(RP)로 계산하므로 마음대로 고를 수 없다.
 */
object DuoSnapshots {
    class Snapshot(
        val tier: String,
        val rp: Int,
        val winRate: Float, // 0..1
        val avgRank: Float,
        val recent: List<Int> // 최근 경기 순위, 최신이 앞 (최대 5)
    )

    /** 게임 닉네임으로 요약을 만든다. 닉네임을 게임 서버에서 못 찾거나 조회가 실패하면 null. */
    suspend fun fetch(nickname: String): Snapshot? {
        val id = GameStats.userId(nickname) ?: return null
        val seasons = GameStats.seasons()
        val current = GameStats.currentSeasonId(seasons) ?: return null
        var list = GameStats.stats(id, current) ?: return null
        if (list.isEmpty()) {
            // 현재 시즌에 기록이 없으면 가장 최근에 기록이 있는 시즌 (최대 3개 시즌)
            list = GameStats.latestWithRecords(id, seasons, current, 3)?.second ?: emptyList()
        }
        val ranked = list.filter { it.matchingMode == GameStats.MODE_RANKED }.maxByOrNull { it.totalGames }
        val shown = ranked ?: GameStats.pickDefault(list)
        val recent = GameStats.games(id)?.games.orEmpty().mapNotNull { it.gameRank?.takeIf { r -> r > 0 } }.take(5)

        return Snapshot(
            tier = if (ranked != null) Tiers.nameOf(ranked.mmr, ranked.rank) else "언랭크",
            rp = ranked?.mmr ?: 0,
            winRate = shown?.top1 ?: 0f,
            avgRank = shown?.averageRank ?: 0f,
            recent = recent
        )
    }
}
