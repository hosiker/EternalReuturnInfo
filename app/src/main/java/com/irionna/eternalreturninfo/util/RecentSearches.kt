package com.irionna.eternalreturninfo.util

import android.content.Context

/**
 * 홈 전적 검색의 최근 검색어 (최대 [MAX]개, 최신순). 이 기기에만 저장하고 서버로 보내지 않는다.
 * 대소문자와 앞뒤 공백이 달라도 같은 닉네임으로 보고 하나만 남긴다.
 */
object RecentSearches {
    const val MAX = 5
    private const val PREFS = "recent_searches"
    private const val KEY = "list"
    private const val SEP = "\n" // 닉네임에는 줄바꿈이 들어갈 수 없다

    fun get(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
        return raw.split(SEP).map { it.trim() }.filter { it.isNotEmpty() }.take(MAX)
    }

    fun add(context: Context, nickname: String) {
        val name = nickname.trim()
        if (name.isEmpty()) return
        val list = listOf(name) + get(context).filterNot { it.equals(name, ignoreCase = true) }
        save(context, list.take(MAX))
    }

    fun remove(context: Context, nickname: String) {
        save(context, get(context).filterNot { it.equals(nickname.trim(), ignoreCase = true) })
    }

    private fun save(context: Context, list: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, list.joinToString(SEP)).apply()
    }
}
