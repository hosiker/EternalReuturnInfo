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

    /** 영상 검색 (호출당 한도 100). order = date(최신순) 또는 viewCount(조회순). */
    @GET("search")
    suspend fun searchVideos(
        @Query("key") apiKey: String = AUTH_KEY,
        @Query("q") query: String,
        @Query("order") order: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("publishedAfter") publishedAfter: String? = null,
        @Query("relevanceLanguage") relevanceLanguage: String = "ko",
        @Query("type") type: String = "video",
        @Query("part") part: String = "snippet",
    ): Response<YoutubeVideo>

    /** 영상 정보(제목·태그·썸네일). ids 는 쉼표로 이은 영상 번호(최대 50개). 호출당 한도 1. */
    @GET("videos")
    suspend fun getVideoDetails(
        @Query("key") apiKey: String = AUTH_KEY,
        @Query("id") ids: String,
        @Query("part") part: String = "snippet",
    ): Response<VideoDetailsResponse>

    /** 채널 업로드 목록의 최신 영상. 검색(호출당 100)과 달리 호출당 한도 1만 쓴다. */
    @GET("playlistItems")
    suspend fun getPlaylistVideos(
        @Query("key") apiKey: String = AUTH_KEY,
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 10,
        @Query("part") part: String = "snippet,contentDetails",
    ): Response<PlaylistItemsResponse>

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


interface FreeCharacterService {
    @GET("v1/freeCharacters/{matchingMode}")
    fun getFreeCharacters(
        @Header("x-api-key") apiKey: String,
        @Path("matchingMode") matchingMode: Int
    ): Call<FreeCharactersResponse>
}


interface UserGamesService {
    @GET("v1/user/games/uid/{userId}")
    fun getUserGames(
        @Header("x-api-key") apiKey: String,
        @Path("userId") userId: String,
        @Query("next") next: Long? = null
    ): Call<UserGamesResponse>
}
