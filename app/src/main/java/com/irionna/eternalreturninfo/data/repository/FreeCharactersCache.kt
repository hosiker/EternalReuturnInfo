package com.irionna.eternalreturninfo.data.repository

import android.content.Context

/** 무료 실험체 번호 목록을 이 기기에 [TTL_MS](6시간) 저장한다. 새로 받기에 실패하면 오래된 저장본이라도 보여준다. */
object FreeCharactersCache {
    const val TTL_MS = 6 * 60 * 60 * 1000L
    private const val PREFS = "free_characters"
    private const val KEY_LIST = "list_v1"
    private const val KEY_TIME = "saved_at_v1"

    class Saved(val list: List<Int>, val savedAt: Long) {
        fun isFresh(now: Long = System.currentTimeMillis()) = now - savedAt in 0 until TTL_MS
    }

    fun load(context: Context): Saved? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = p.getString(KEY_LIST, null) ?: return null
        val list = raw.split(",").mapNotNull { it.trim().toIntOrNull() }
        return if (list.isEmpty()) null else Saved(list, p.getLong(KEY_TIME, 0L))
    }

    fun save(context: Context, list: List<Int>) {
        if (list.isEmpty()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LIST, list.joinToString(","))
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }
}
