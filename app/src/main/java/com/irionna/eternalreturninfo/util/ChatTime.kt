package com.irionna.eternalreturninfo.util

import android.content.Context
import com.irionna.eternalreturninfo.R
import com.irionna.eternalreturninfo.data.model.Message
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 채팅 시각 처리.
 * 새 메시지는 숫자 시각(ts)과 예전과 같은 모양의 글자(time, "2026년 11월 09일 오후 03시 05분")를 함께 저장한다.
 * 옛 메시지는 글자만 있어서 읽어서 숫자로 바꾼다. (안드로이드 8 미만에서 종료되던 LocalDateTime 은 쓰지 않는다)
 */
object ChatTime {
    private const val LEGACY = "yyyy년 MM월 dd일 a hh시 mm분"

    /** 저장용 글자 */
    fun legacyText(millis: Long): String = SimpleDateFormat(LEGACY, Locale.KOREA).format(Date(millis))

    /** 옛 형식 글자를 숫자 시각으로. 읽을 수 없으면 null. (기기 언어에 따라 오전/오후가 달라서 몇 가지 언어로 시도) */
    fun parseLegacy(time: String?): Long? {
        if (time.isNullOrBlank()) return null
        for (locale in listOf(Locale.KOREA, Locale.getDefault(), Locale.ENGLISH)) {
            try {
                SimpleDateFormat(LEGACY, locale).parse(time)?.time?.let { return it }
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun millisOf(m: Message): Long? = m.ts ?: parseLegacy(m.time)

    /** "오후 3:05" */
    fun clock(millis: Long): String = SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(millis))

    private fun dayKey(millis: Long): Int {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    fun sameDay(a: Long, b: Long) = dayKey(a) == dayKey(b)

    /** 채팅방 날짜 구분선: "오늘", "어제", "11월 9일 일요일" */
    fun dayLabel(context: Context, millis: Long, now: Long = System.currentTimeMillis()): String = when {
        sameDay(millis, now) -> context.getString(R.string.chat_day_today)
        sameDay(millis, now - 24L * 60 * 60 * 1000) -> context.getString(R.string.chat_day_yesterday)
        else -> SimpleDateFormat("M월 d일 EEEE", Locale.KOREA).format(Date(millis))
    }

    /** 채팅 목록 시각: "방금", "12분 전", "3시간 전", "어제", "11월 7일" */
    fun listLabel(context: Context, millis: Long, now: Long = System.currentTimeMillis()): String {
        if (millis <= 0) return ""
        val min = (now - millis) / 60_000
        return when {
            min < 1 -> context.getString(R.string.rg_time_now)
            min < 60 -> context.getString(R.string.rg_time_min, min.toInt())
            sameDay(millis, now) -> context.getString(R.string.rg_time_hour, (min / 60).toInt())
            sameDay(millis, now - 24L * 60 * 60 * 1000) -> context.getString(R.string.chat_day_yesterday)
            else -> SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(millis))
        }
    }
}
