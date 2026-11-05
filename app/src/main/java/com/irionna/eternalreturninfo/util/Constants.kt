package com.irionna.eternalreturninfo.util

import com.irionna.eternalreturninfo.BuildConfig

class Constants {
    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
        const val AUTH_KEY = BuildConfig.YOUTUBE_APIKEY // 호식
        const val EXTRA_ER_MODEL = "extra_er_model"
        const val EXTRA_ER_POSITION = "extra_er_position"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_TIME = "extra_time"

        // 개인정보처리방침 공개 주소 (docs/privacy-policy.md 를 GitHub Pages 로 공개한 것)
        const val PRIVACY_POLICY_URL = "https://hosiker.github.io/EternalReuturnInfo/privacy-policy"

        // 이용약관(커뮤니티 규칙) 공개 주소 (docs/terms.md)
        const val TERMS_URL = "https://hosiker.github.io/EternalReuturnInfo/terms"

        const val MAIN_BASE_URL = "https://open-api.bser.io"
        const val MAIN_APIKEY = BuildConfig.MAIN_APIKEY

        const val ETERNAL_RETURN_BASE_URL = "https://playeternalreturn.com"
    }

}