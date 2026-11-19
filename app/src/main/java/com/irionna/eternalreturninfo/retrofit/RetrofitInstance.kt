package com.irionna.eternalreturninfo.retrofit

import com.irionna.eternalreturninfo.BuildConfig
import com.irionna.eternalreturninfo.util.Constants
import com.irionna.eternalreturninfo.util.Constants.Companion.BASE_URL
import com.irionna.eternalreturninfo.util.Constants.Companion.ETERNAL_RETURN_BASE_URL
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object RetrofitInstance {
    /**
     * 게임 서버 주소 중 IPv4를 먼저 시도한다. 일부 폰 네트워크에서 IPv6 연결이 막혀 있으면 요청마다
     * 연결 제한 시간만큼 기다린 뒤에야 IPv4로 넘어가서 전적 조회가 매우 느려진다. (브라우저는 알아서 건너뛴다)
     */
    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            Dns.SYSTEM.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 }
    }

    /** 이터널 리턴 Open API용 공통 클라이언트: IPv4 우선 + 짧은 제한 시간(오래 매달리지 않고 실패 안내를 보여주기 위해). */
    private val openApiClient by lazy {
        OkHttpClient.Builder()
            .dns(ipv4FirstDns)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api : YoutubeApiService by lazy {
        retrofit.create(YoutubeApiService::class.java)
    }

    private val eternalRetrofit by lazy {
        val okHttpClient = OkHttpClient.Builder().addInterceptor(
            HttpLoggingInterceptor(logger = HttpLoggingInterceptor.Logger.DEFAULT).apply {
                // 응답 본문까지 로그에 남기는 것은 디버그 빌드에서만 한다.
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        ).build()

        Retrofit.Builder()
            .baseUrl(ETERNAL_RETURN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val eternalApi by lazy {
        eternalRetrofit.create(ApiService::class.java)
    }

    private val userRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.MAIN_BASE_URL)
            .client(openApiClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val searchUserIDApi by lazy {
        userRetrofit.create(UserService::class.java)
    }

    val searchUserStateApi by lazy {
        userRetrofit.create(UserStatsService::class.java)
    }

    val seasonApi by lazy {
        userRetrofit.create(SeasonService::class.java)
    }

    val freeCharacterApi by lazy {
        userRetrofit.create(FreeCharacterService::class.java)
    }

    val userGamesApi by lazy {
        userRetrofit.create(UserGamesService::class.java)
    }

}
