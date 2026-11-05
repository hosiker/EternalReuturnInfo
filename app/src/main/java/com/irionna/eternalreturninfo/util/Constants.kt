package com.irionna.eternalreturninfo.util

import com.irionna.eternalreturninfo.BuildConfig

class Constants {
    companion object {
        // 홈 "게임 방송"에 보여줄 공식 채널 ("이터널 리턴", YouTube 검색으로 확인).
        // 채널 번호가 UC 로 시작하면 업로드 목록 번호는 UU + 나머지 글자이다.
        const val OFFICIAL_CHANNEL_ID = "UCEOaB76vS9RfiAwEzxB8QGw"
        val OFFICIAL_UPLOADS_PLAYLIST: String get() = "UU" + OFFICIAL_CHANNEL_ID.substring(2)

        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
        val AUTH_KEY: String get() = BuildConfig.YOUTUBE_APIKEY // const 로 두면 키를 바꿔도 이전 값이 남을 수 있다
        const val EXTRA_ER_MODEL = "extra_er_model"
        const val EXTRA_ER_POSITION = "extra_er_position"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_TIME = "extra_time"

        // 개인정보처리방침 공개 주소 (docs/privacy-policy.md 를 GitHub Pages 로 공개한 것)
        const val PRIVACY_POLICY_URL = "https://hosiker.github.io/EternalReuturnInfo/privacy-policy"

        // 이용약관(커뮤니티 규칙) 공개 주소 (docs/terms.md)
        const val TERMS_URL = "https://hosiker.github.io/EternalReuturnInfo/terms"

        const val MAIN_BASE_URL = "https://open-api.bser.io"
        val MAIN_APIKEY: String get() = BuildConfig.MAIN_APIKEY

        const val ETERNAL_RETURN_BASE_URL = "https://playeternalreturn.com"
    }

}