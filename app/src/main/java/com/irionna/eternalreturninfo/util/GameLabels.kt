package com.irionna.eternalreturninfo.util

import android.content.Context
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.repository.GameStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 경기 기록 화면에서 쓰는 글자 만들기: 모드 이름, 몇 분 전 같은 상대 시각. */
object GameLabels {
    fun mode(context: Context, mode: Int?, team: Int?): String {
        val m = when (mode) {
            GameStats.MODE_RANKED -> context.getString(R.string.stats_mode_ranked)
            GameStats.MODE_NORMAL -> context.getString(R.string.stats_mode_normal)
            else -> context.getString(R.string.stats_mode_other, mode ?: 0)
        }
        val t = when (team) {
            1 -> context.getString(R.string.stats_team_solo)
            2 -> context.getString(R.string.stats_team_duo)
            3 -> context.getString(R.string.stats_team_squad)
            else -> return m
        }
        return "$m $t"
    }

    /** "2026-08-17T13:31:17.017+0900" 형식의 시작 시각을 "3시간 전" 같은 글자로 바꾼다. 읽을 수 없으면 빈 글자. */
    fun relativeTime(context: Context, iso: String?, now: Long = System.currentTimeMillis()): String {
        val start = try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).parse(iso ?: return "")?.time ?: return ""
        } catch (e: Exception) {
            return ""
        }
        val min = (now - start) / 60_000
        return when {
            min < 1 -> context.getString(R.string.rg_time_now)
            min < 60 -> context.getString(R.string.rg_time_min, min.toInt())
            min < 60 * 24 -> context.getString(R.string.rg_time_hour, (min / 60).toInt())
            min < 60 * 24 * 7 -> context.getString(R.string.rg_time_day, (min / (60 * 24)).toInt())
            else -> context.getString(R.string.rg_time_date, SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(start)))
        }
    }
}
