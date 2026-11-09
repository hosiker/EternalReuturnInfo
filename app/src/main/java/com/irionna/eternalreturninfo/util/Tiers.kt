package com.irionna.eternalreturninfo.util

/**
 * RP(mmr)로 랭크 티어 이름을 구한다. 서버 전적 응답에는 티어 이름이 없다. (세부 단계 I~IV 는 표시하지 않는다)
 *
 * 기준표 출처와 확인 상태 (2026-11-09 점검). 시즌마다 바뀔 수 있어서 게임 화면과 다르면 [LOWER_TIERS] 의 숫자만 고치면 된다.
 *  - 공식 확인: 미스릴 시작 7,600 RP (공식 패치노트 11.1, 시즌 11), 메테오라이트 3·2·1 시작 6,700·7,000·7,300 RP,
 *    데미갓·이터니티는 미스릴에 도달한 상위 301~1,000위 / 상위 300위이며 최소 7,900 RP (시즌 9 보상 공지).
 *  - 메테오라이트 시작 6,400 RP 는 공식 간격(300)으로 맞춰 본 값이고, 아이언~다이아몬드 숫자(팬 위키 기준)는 공식 표를 못 찾아 미확인이다.
 *  - 현재 시즌(시즌 12·13)의 값은 확인하지 못했다.
 */
object Tiers {
    /** (이 점수 이상, 티어 이름) — 오름차순 */
    private val LOWER_TIERS = listOf(
        0 to "아이언",
        600 to "브론즈",
        1400 to "실버",
        2400 to "골드",
        3600 to "플래티넘",
        5000 to "다이아몬드",
        6400 to "메테오라이트",
        7600 to "미스릴"
    )

    private const val TOP_TIER_MIN_RP = 7900
    private const val ETERNITY_MAX_RANK = 300
    private const val DEMIGOD_MAX_RANK = 1000

    /** 순위가 0 이하면 순위 정보가 없는 것으로 보고 점수만으로 정한다 (미스릴까지). */
    fun nameOf(mmr: Int, rank: Int): String {
        if (mmr <= 0) return "언랭크"
        if (mmr >= TOP_TIER_MIN_RP && rank in 1..DEMIGOD_MAX_RANK) {
            return if (rank <= ETERNITY_MAX_RANK) "이터니티" else "데미갓"
        }
        return LOWER_TIERS.last { mmr >= it.first }.second
    }

    /** 듀오 모집글에서 직접 고를 수 있는 티어 이름 (전적에서 못 가져왔을 때) */
    val SELECTABLE: List<String> = listOf("언랭크") + LOWER_TIERS.map { it.second } + listOf("데미갓", "이터니티")

    /** 예전 모집글에 저장된 영어 티어 이름을 지금 이름으로 바꾼다. 모르는 값은 그대로. */
    fun displayName(raw: String?): String = when (raw?.trim()) {
        null, "" -> ""
        "Unrank" -> "언랭크"
        "Iron" -> "아이언"
        "Bronze" -> "브론즈"
        "Silver" -> "실버"
        "Gold" -> "골드"
        "Platinum" -> "플래티넘"
        "Diamond" -> "다이아몬드"
        "Meteorite" -> "메테오라이트"
        "Mithril" -> "미스릴"
        "Titan" -> "데미갓"
        "Immortal" -> "이터니티"
        else -> raw.trim()
    }
}
