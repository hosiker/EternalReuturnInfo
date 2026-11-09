package com.irionna.eternalreturninfo.util

/**
 * 쇼츠 구분. YouTube 는 쇼츠 여부를 직접 알려 주지 않아서 영상 길이로 판단한다.
 *  - 60초 이하: 쇼츠
 *  - 61~180초: 제목이나 태그에 #shorts 가 있을 때만 쇼츠 (쇼츠는 3분까지 올릴 수 있지만, 같은 길이의 일반 영상도 많다)
 *  - 그 외, 길이를 모르면: 일반 영상
 */
object VideoKind {
    private const val SHORT_SURE_SEC = 60
    private const val SHORT_MAX_SEC = 180

    /** "PT1H2M3S" 같은 길이를 초로 바꾼다. 읽을 수 없으면 null. */
    fun seconds(iso: String?): Int? {
        val m = Regex("^P(?:(\\d+)D)?T?(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?$").matchEntire(iso ?: return null) ?: return null
        val (d, h, min, s) = m.destructured
        if (d.isEmpty() && h.isEmpty() && min.isEmpty() && s.isEmpty()) return null
        return (d.toIntOrNull() ?: 0) * 86400 + (h.toIntOrNull() ?: 0) * 3600 + (min.toIntOrNull() ?: 0) * 60 + (s.toIntOrNull() ?: 0)
    }

    fun isShort(duration: String?, title: String?, tags: List<String>?): Boolean {
        val sec = seconds(duration) ?: return false
        if (sec <= 0) return false // 예정된 생방송 등은 길이가 0
        if (sec <= SHORT_SURE_SEC) return true
        if (sec > SHORT_MAX_SEC) return false
        val text = (title.orEmpty() + " " + tags.orEmpty().joinToString(" ")).lowercase()
        return text.contains("#shorts") || text.contains("#short") || tags.orEmpty().any { it.equals("shorts", true) }
    }
}
