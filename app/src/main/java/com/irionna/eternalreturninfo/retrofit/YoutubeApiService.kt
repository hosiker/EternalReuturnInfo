package com.irionna.eternalreturninfo.retrofit

import com.irionna.eternalreturninfo.util.Constants.Companion.AUTH_KEY
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query


interface YoutubeApiService {
    @GET("search")
    suspend fun getYouTubeVideos(
        @Query("key")apiKey: String = AUTH_KEY,
        @Query("q")query: String,
        @Query("order")videoOrder: String,
        @Query("type")videoType: String = "video",
        @Query("maxResults")maxResults: Int,
        @Query("channelId")channelId: String? = null,
        @Query("part")part: String = "snippet",
    ): Response<YoutubeVideo>

    @GET("search")
    suspend fun getYouTubeMoreVideos(
        @Query("key") apiKey: String = AUTH_KEY,
        @Query("q") query: String,
        @Query("pageToken") nextPageToken: String,
        @Query("order") videoOrder: String,
        @Query("type") videoType: String = "video",
        @Query("maxResults") maxResults: Int,
        @Query("channelId") channelId: String? = null,
        @Query("part") part: String = "snippet",
    ): Response<YoutubeVideo>
}

interface ApiService {
    @GET("/api/v1/posts/news")
    fun getNews(@Header("Cookie") locale: String): Call<ResponseModel>
}

interface UserStatsService {
    /** 새 방식: 닉네임 조회가 주는 userId 로 전적을 받는다. */
    @GET("v1/user/stats/uid/{userId}/{seasonId}")
    fun getUserStatsByUid(
        @Header("x-api-key") apiKey: String,
        @Path("userId") userId: String,
        @Path("seasonId") seasonId: String
    ): Call<UserStatsResponse>
}

interface UserService {
    @GET("v1/user/nickname")
    fun getUserByNickname(
        @Header("x-api-key") apiKey: String,
        @Query("query") query: String?
    ): Call<UserResponse>
}

interface SeasonService {
    @GET("v2/data/Season")
    fun getSeasons(@Header("x-api-key") apiKey: String): Call<SeasonResponse>
}
