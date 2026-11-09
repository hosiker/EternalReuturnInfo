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

    /** 게시판 글 검색의 최근 검색어는 닉네임 목록과 섞이지 않도록 [scope] 를 따로 쓴다. */
    const val SCOPE_BOARD = "board_list"

    fun get(context: Context, scope: String = KEY): List<String> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(scope, "").orEmpty()
        return raw.split(SEP).map { it.trim() }.filter { it.isNotEmpty() }.take(MAX)
    }

    fun add(context: Context, nickname: String, scope: String = KEY) {
        val name = nickname.trim()
        if (name.isEmpty()) return
        val list = listOf(name) + get(context, scope).filterNot { it.equals(name, ignoreCase = true) }
        save(context, list.take(MAX), scope)
    }

    fun remove(context: Context, nickname: String, scope: String = KEY) {
        save(context, get(context, scope).filterNot { it.equals(nickname.trim(), ignoreCase = true) }, scope)
    }

    fun clear(context: Context, scope: String = KEY) = save(context, emptyList(), scope)

    private fun save(context: Context, list: List<String>, scope: String = KEY) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(scope, list.joinToString(SEP)).apply()
    }
}
