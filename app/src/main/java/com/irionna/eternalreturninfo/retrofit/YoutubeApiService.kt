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
        @Query("channelId")channelId: String = "",
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
        @Query("channelId") channelId: String = "",
        @Query("part") part: String = "snippet",
    ): Response<YoutubeVideo>
}

interface ApiService {
    @GET("/api/v1/posts/news")
    fun getNews(@Header("Cookie") locale: String): Call<ResponseModel>
}

interface UserStatsService {
    @GET("v1/user/stats/{userNum}/{seasonId}")
    fun getUserStats(
        @Header("x-api-key") apiKey: String,
        @Path("userNum") userNum: String,
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
