package com.irionna.eternalreturninfo.util

import android.content.Context
import com.irionna.eternalreturninfo.R

/**
 * 게임 서버가 주는 실험체 번호(characterCode)와 앱의 실험체 목록(R.array.characterName)을 이어 준다.
 *
 * 앱 목록의 순서는 게임 번호와 7곳에서 다르다 (예: 게임 3번=피오라, 앱 3번=현우).
 * 프로필·가입은 앱 목록 위치로 저장하므로 그대로 두고, **서버가 준 번호를 화면에 쓸 때만** [appIndexOf] 로 바꾼다.
 * (이름은 게임 한글 이름 데이터 l10n 의 Character/Name/{번호} 와 대조해 확인했다)
 */
object Characters {
    /** 게임 번호 → 앱 목록 위치. 위치가 같은 번호는 적지 않는다. */
    private val CODE_TO_APP = mapOf(3 to 5, 5 to 7, 7 to 3, 14 to 15, 15 to 14, 16 to 17, 17 to 16)

    /** 앱에 이름·얼굴 이미지가 있는 마지막 게임 번호 (레니). 이보다 큰 번호는 신규 실험체다. */
    private const val LAST_APP_CODE = 69

    /** 앱에 이미지가 없는 신규 실험체의 한글 이름 (게임 번호 → 이름, 게임 이름 데이터 기준). 새 실험체가 나오면 여기에 더한다. */
    private val NEW_NAMES = mapOf(
        70 to "츠바메", 71 to "케네스", 72 to "카티야", 73 to "샬럿", 74 to "다르코", 75 to "르노어",
        76 to "가넷", 77 to "유민", 78 to "히스이", 79 to "유스티나", 80 to "이슈트반", 81 to "니아",
        82 to "슈린", 83 to "헨리", 84 to "블레어", 85 to "미르카", 86 to "펜리르", 87 to "코렐라인",
        88 to "비형", 89 to "크레이버", 90 to "루치아", 91 to "세레스"
    )

    /** 서버가 준 번호에 해당하는 앱 목록 위치. 앱에 없는 신규 실험체면 null. */
    fun appIndexOf(code: Int): Int? = if (code in 1..LAST_APP_CODE) CODE_TO_APP[code] ?: code else null

    /** 화면에 쓸 이름. 앱 목록 → 신규 이름표 → "#번호" 순서. */
    fun nameOf(context: Context, code: Int): String {
        val index = appIndexOf(code)
        if (index != null) {
            return context.resources.getStringArray(R.array.characterName).getOrNull(index) ?: "#$code"
        }
        return NEW_NAMES[code] ?: "#$code"
    }
}
