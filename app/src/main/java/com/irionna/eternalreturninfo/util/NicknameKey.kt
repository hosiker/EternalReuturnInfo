package com.irionna.eternalreturninfo.util

/**
 * 닉네임 중복 판단에 쓰는 키(Firestore `nicknames/{키}` 문서 ID)를 만든다.
 * 앞뒤 공백을 지우고 소문자로 맞춰서, 대소문자만 다른 닉네임도 같은 닉네임으로 본다.
 * Firestore 문서 ID 로 쓸 수 없는 닉네임(빈 값, '/' 포함, 너무 긴 값, 예약된 형태)은 null 을 돌려준다.
 */
object NicknameKey {
    private const val MAX_LENGTH = 100

    fun normalize(nickname: String?): String? {
        val key = nickname?.trim()?.lowercase() ?: return null
        if (key.isEmpty() || key.length > MAX_LENGTH) return null
        if (key.contains('/')) return null
        if (key == "." || key == "..") return null
        if (key.startsWith("__") && key.endsWith("__")) return null
        return key
    }
}
