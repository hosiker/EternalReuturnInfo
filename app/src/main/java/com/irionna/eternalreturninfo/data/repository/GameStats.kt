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

    /** 요청별 실패 이유 (응답 본문의 code, 없으면 HTTP 코드). 화면 안내 문구를 고르는 데 쓴다. 요청마다 따로 보관해 동시 요청이 서로 덮어쓰지 않는다. */
    private val statsErrors = HashMap<String, Int>()

    fun statsError(userId: String, seasonId: Int): Int? = statsErrors["$userId/$seasonId"]

    private val gate = Mutex()
    private var lastCallAt = 0L

    private val userIds = HashMap<String, String>()
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

    /** 닉네임으로 게임 유저 id 를 찾는다. (서버가 userNum 대신 userId 문자열을 준다) */
    suspend fun userId(nickname: String): String? {
        if (nickname.isBlank() || !keyReady()) return null
        val cacheKey = nickname.trim().lowercase()
        userIds[cacheKey]?.let { return it }
        val r = throttled { RetrofitInstance.searchUserIDApi.getUserByNickname(Constants.MAIN_APIKEY, nickname.trim()).execute() }
        val id = if (r?.isSuccessful == true) r.body()?.user?.userId?.takeIf { it.isNotBlank() } else null
        if (id != null) userIds[cacheKey] = id
        else if (BuildConfig.DEBUG) Log.d(TAG, "유저 id 조회 실패: HTTP ${r?.code()} code=${r?.body()?.code}")
        return id
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
    suspend fun stats(userId: String, seasonId: Int): List<UserStats>? {
        val key = "$userId/$seasonId"
        statsCache[key]?.let { return it }
        val r = throttled {
            RetrofitInstance.searchUserStateApi.getUserStatsByUid(Constants.MAIN_APIKEY, userId, seasonId.toString()).execute()
        }
        if (r == null) return null
        if (BuildConfig.DEBUG) Log.d(TAG, "전적 요청 season=$seasonId → HTTP ${r.code()}")
        if (!r.isSuccessful) {
            // 기록이 없는 시즌은 404 로 온다. 이 경우는 "기록 없음"으로 본다.
            if (BuildConfig.DEBUG) Log.d(TAG, "전적 조회 HTTP ${r.code()} (season=$seasonId)")
            statsErrors[key] = r.code()
            return if (r.code() == 404) emptyList<UserStats>().also { statsCache[key] = it } else null
        }
        // 서버는 HTTP 200 이어도 본문 code 로 실패를 알린다 (200 성공, 404 기록 없음, 401/403 권한 없음 등)
        val bodyCode = r.body()?.code
        if (bodyCode != null && bodyCode != 200) {
            if (bodyCode == 404) return emptyList<UserStats>().also { statsCache[key] = it }
            statsErrors[key] = bodyCode
            return null
        }
        statsErrors.remove(key)
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

    /**
     * [fromSeasonId] 보다 이전 시즌 중 기록이 있는 가장 최근 시즌을 찾는다. (최대 [maxTries] 개 시즌만 조회)
     * 조회가 실패하면 null. 기록 있는 시즌이 없어도 null.
     */
    suspend fun latestWithRecords(
        userId: String,
        seasons: List<SeasonInfo>,
        fromSeasonId: Int,
        maxTries: Int
    ): Pair<SeasonInfo, List<UserStats>>? {
        for (season in seasons.filter { it.id < fromSeasonId }.take(maxTries)) {
            val list = stats(userId, season.id) ?: return null
            if (list.isNotEmpty()) return season to list
        }
        return null
    }

    /**
     * 이번에 무료인 실험체 번호 목록 (일반 게임 기준, 랭크는 로테이션이 없다). 실패하면 null.
     * 요청 줄서기는 다른 전적 요청과 같이 쓴다.
     */
    suspend fun freeCharacters(): List<Int>? {
        if (!keyReady()) return null
        val r = throttled { RetrofitInstance.freeCharacterApi.getFreeCharacters(Constants.MAIN_APIKEY, MODE_NORMAL).execute() }
        if (r?.isSuccessful != true) {
            if (BuildConfig.DEBUG) Log.d(TAG, "무료 실험체 조회 실패: HTTP ${r?.code()}")
            return null
        }
        val body = r.body()
        if (body?.code != null && body.code != 200) return null
        return body?.freeCharacters.orEmpty()
    }

    /** 듀오 찾기 카드용: 현재 시즌의 기본 기록 한 줄 요약. */
    suspend fun summary(nickname: String): UserStats? {
        val id = userId(nickname) ?: return null
        val all = seasons()
        val seasonId = currentSeasonId(all) ?: return null
        val current = stats(id, seasonId) ?: return null
        if (current.isNotEmpty()) return pickDefault(current)
        // 현재 시즌 기록이 없으면 가장 최근에 기록이 있는 시즌 (카드마다 호출하므로 3개 시즌까지만)
        return latestWithRecords(id, all, seasonId, 3)?.second?.let { pickDefault(it) }
    }
}
