package com.irionna.eternalreturninfo.util

/**
 * 스트리머 탭에 보여줄 영상을 이터널 리턴 관련으로만 거른다.
 * 영상 제목과 태그에 아래 단어가 하나라도 들어 있으면 통과. 대소문자와 띄어쓰기는 무시한다.
 * 단어를 더하거나 빼려면 [KEYWORDS] 만 고친다. ("이리", "리턴" 같은 짧은 단어는 관련 없는 영상이 걸릴 수 있다)
 */
object VideoFilter {
    private val KEYWORDS = listOf(
        "이터널리턴", "eternalreturn", "이터널", "리턴", "블서", "블랙서바이벌", "영회", "영원회귀", "이리"
    )

    private fun normalize(s: String) = s.lowercase().filterNot { it.isWhitespace() }

    fun matches(title: String?, tags: List<String>?): Boolean {
        val text = normalize(title.orEmpty() + " " + tags.orEmpty().joinToString(" "))
        return KEYWORDS.any { text.contains(it) }
    }

    /** 검색어: YouTube 가 "|" 를 "또는" 으로 처리한다. */
    const val SEARCH_QUERY = "이터널리턴|이터널 리턴|eternal return|블서|영회"
}
