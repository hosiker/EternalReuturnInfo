package com.irionna.eternalreturninfo.data.repository

import android.os.SystemClock
import android.util.Log
import com.irionna.eternalreturninfo.BuildConfig
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.retrofit.RetrofitInstance
import com.irionna.eternalreturninfo.retrofit.UserStats
import com.irionna.eternalreturninfo.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

/**
 * 게임 공식 API(전적) 조회를 한 곳에 모은 저장소. 홈·프로필·듀오 찾기 카드가 모두 이 코드를 쓴다.
 *  - 요청은 줄을 세워 일정 간격(MIN_INTERVAL_MS)을 두고 보낸다. (듀오 목록이 한꺼번에 여러 번 부르면 제한에 걸리므로)
 *  - 닉네임→유저 번호, 시즌 목록, 시즌별 전적은 앱이 켜져 있는 동안 메모리에 보관해 다시 부르지 않는다.
 *  - 모든 함수는 실패하면 null/빈 목록을 돌려준다. 호출하는 쪽에서 안내 문구를 보여준다.
 */
object GameStats {

    data class SeasonInfo(val id: Int, val name: String, val isCurrent: Boolean)

    // 랭크/일반 구분 (matchingMode), 솔로/듀오/스쿼드 구분 (matchingTeamMode)
    // 값의 뜻은 점검 로그(ERStats)와 공식 문서로 확인한다.
    const val MODE_NORMAL = 2
    const val MODE_RANKED = 3

    private const val MIN_INTERVAL_MS = 1000L
    private const val TAG = "ERStats"

    private val gate = Mutex()
    private var lastCallAt = 0L

    private val userNums = HashMap<String, Long>()
    private var seasonCache: List<SeasonInfo>? = null
    private val statsCache = HashMap<String, List<UserStats>>()

    /** 줄을 세워 일정 간격으로 요청한다. 429(너무 많은 요청)면 한 번 더 시도한다. 네트워크 오류는 null. */
    private suspend fun <T> throttled(block: () -> Response<T>): Response<T>? = gate.withLock {
        withContext(Dispatchers.IO) {
            var result: Response<T>? = null
            for (attempt in 0..1) {
                val wait = lastCallAt + MIN_INTERVAL_MS - SystemClock.elapsedRealtime()
                if (wait > 0) delay(wait)
                val r = try {
                    block()
                } catch (e: IOException) {
                    lastCallAt = SystemClock.elapsedRealtime()
                    return@withContext null
                }
                lastCallAt = SystemClock.elapsedRealtime()
                result = r
                if (r.code() != 429) break
            }
            result
        }
    }

    private fun keyReady() = Constants.MAIN_APIKEY.isNotBlank()

    suspend fun userNum(nickname: String): Long? {
        if (nickname.isBlank() || !keyReady()) return null
        userNums[nickname]?.let { return it }
        val r = throttled { RetrofitInstance.searchUserIDApi.getUserByNickname(Constants.MAIN_APIKEY, nickname).execute() }
        val num = if (r?.isSuccessful == true) r.body()?.user?.userNum else null
        if (num != null) userNums[nickname] = num
        else if (BuildConfig.DEBUG) Log.d(TAG, "유저 번호 조회 실패: HTTP ${r?.code()}")
        return num
    }

    /** 시즌 목록(최신이 위). 서버 목록을 못 받으면 데이터베이스의 현재 시즌 값 하나로 대신한다. */
    suspend fun seasons(): List<SeasonInfo> {
        seasonCache?.let { return it }
        if (keyReady()) {
            val r = throttled { RetrofitInstance.seasonApi.getSeasons(Constants.MAIN_APIKEY).execute() }
            val list = if (r?.isSuccessful == true) {
                r.body()?.data.orEmpty().mapNotNull { d ->
                    val id = d.seasonID ?: return@mapNotNull null
                    SeasonInfo(id, d.seasonName?.takeIf { it.isNotBlank() } ?: "시즌 $id", d.isCurrent == 1)
                }.sortedByDescending { it.id }
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "시즌 목록 조회 실패: HTTP ${r?.code()}")
                emptyList()
            }
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "시즌 목록 ${list.size}개: " + list.joinToString { "${it.id}=${it.name}${if (it.isCurrent) "(현재)" else ""}" })
            }
            if (list.isNotEmpty()) {
                seasonCache = list
                return list
            }
        }
        val fallback = BoardSingletone.seasonID().toIntOrNull()
        return if (fallback != null) listOf(SeasonInfo(fallback, "시즌 $fallback", true)) else emptyList()
    }

    fun currentSeasonId(seasons: List<SeasonInfo>): Int? =
        seasons.firstOrNull { it.isCurrent }?.id ?: seasons.firstOrNull()?.id

    /** 한 시즌의 전적 목록(모드별 기록). 실패하면 null, 기록이 없으면 빈 목록. */
    suspend fun stats(userNum: Long, seasonId: Int): List<UserStats>? {
        val key = "$userNum/$seasonId"
        statsCache[key]?.let { return it }
        val r = throttled {
            RetrofitInstance.searchUserStateApi.getUserStats(Constants.MAIN_APIKEY, userNum.toString(), seasonId.toString()).execute()
        }
        if (r == null) return null
        if (BuildConfig.DEBUG) Log.d(TAG, "전적 요청 season=$seasonId → HTTP ${r.code()}")
        if (!r.isSuccessful) {
            // 기록이 없는 시즌은 404 로 온다. 이 경우는 "기록 없음"으로 본다.
            if (BuildConfig.DEBUG) Log.d(TAG, "전적 조회 HTTP ${r.code()} (season=$seasonId)")
            return if (r.code() == 404) emptyList<UserStats>().also { statsCache[key] = it } else null
        }
        val list = r.body()?.userStats.orEmpty()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "응답 본문 code=${r.body()?.code} message=${r.body()?.message}")
            Log.d(TAG, "season=$seasonId 기록 ${list.size}개")
            list.forEach {
                Log.d(TAG, "  season=${it.seasonId} mode=${it.matchingMode} team=${it.matchingTeamMode} mmr=${it.mmr} rank=${it.rank} games=${it.totalGames}")
            }
        }
        statsCache[key] = list
        return list
    }

    /** 기본으로 보여줄 기록: 랭크 중 게임 수가 가장 많은 것, 없으면 전체 중 가장 많은 것. */
    fun pickDefault(list: List<UserStats>): UserStats? =
        list.filter { it.matchingMode == MODE_RANKED }.maxByOrNull { it.totalGames }
            ?: list.maxByOrNull { it.totalGames }

    /** 듀오 찾기 카드용: 현재 시즌의 기본 기록 한 줄 요약. */
    suspend fun summary(nickname: String): UserStats? {
        val num = userNum(nickname) ?: return null
        val seasonId = currentSeasonId(seasons()) ?: return null
        return stats(num, seasonId)?.let { pickDefault(it) }
    }
}
