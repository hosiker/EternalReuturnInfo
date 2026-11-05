package com.irionna.eternalreturninfo.retrofit

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class YoutubeVideo(
    @SerializedName("kind")
    val kind: String,
    @SerializedName("etag")
    val etag: String,
    @SerializedName("nextPageToken")
    val nextPageToken: String? = null,
    @SerializedName("regionCode")
    val regionCode: String? = null,
    @SerializedName("pageInfo")
    val pageInfo: PageInfo? = null,
    @SerializedName("items")
    val items: List<Items>?,
)

@Keep
data class PageInfo(
    @SerializedName("totalResults")
    val totalResults: Int,
    @SerializedName("resultsPerPage")
    val resultsPerPage: Int
)
@Keep
data class Items(
    @SerializedName("id")
    val id: Id,
    @SerializedName("snippet")
    val snippet: Snippet
)
@Keep
data class Id(
    @SerializedName("kind")
    val kind: String,
    @SerializedName("videoId")
    val videoId: String? = null
)
@Keep
data class Snippet(
    // 검색 결과에는 없는 필드가 있어서 모두 nullable (categoryId 는 검색 응답에 없다)
    @SerializedName("categoryId")
    val categoryId: String? = null,
    @SerializedName("publishedAt")
    val publishedAt: String? = null,
    @SerializedName("channelId")
    val channelId: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("thumbnails")
    val thumbnails: ThumbNail? = null,
    @SerializedName("publishTime")
    val publishTime: String? = null,
    @SerializedName("channelTitle")
    val channelTitle: String? = null,
)
@Keep
data class ThumbNail(
    @SerializedName("medium")
    val medium: Medium? = null
)
@Keep
data class Medium(
    @SerializedName("url")
    val url: String,
    @SerializedName("width")
    val width: Int,
    @SerializedName("height")
    val height: Int
)
@Keep
data class YoutubeVideoInfo(
    @SerializedName("kind")
    val kind: String,
    @SerializedName("etag")
    val etag: String,
    @SerializedName("items")
    val items: List<TrendItem>?,
    @SerializedName("nextPageToken")
    val nextPageToken: String?
)
@Keep
data class TrendItem(
    @SerializedName("kind")
    val kind: String,
    @SerializedName("etag")
    val etag: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("snippet")
    val snippet: Snippet,
    @SerializedName("tags")
    val tags: List<String>,
    @SerializedName("contentDetails")
    val contentDetails: ContentDetails,
    @SerializedName("statistics")
    val statistics: Statistics
)
@Keep
data class ContentDetails(
    @SerializedName("duration")
    val duration: String
)
@Keep
data class Statistics(
    @SerializedName("viewCount")
    val viewCount: String? = ""
)
@Keep
data class ResponseModel(
    val per_page: Int,
    val current_page: Int,
    val total_page: Int,
    val articles: List<Article>
)
@Keep
data class Article(
    val id: Int,
    val board_id: Int,
    val category_id: Int,
    val thumbnail_url: String,
    val view_count: Int,
    val is_hidden: Int,
    val is_pinned: Boolean,
    val created_at: String,
    val updated_at: String,
    val i18ns: Map<String, I18n>,
    val url: String
)
@Keep
data class I18n(
    val locale: String,
    val title: String,
    val summary: String,
    val created_at_for_humans: String
)
@Keep
data class UserResponse(
    val code: Int,
    val message: String,
    val user: User
)
@Keep
data class User(
    // 서버가 유저 식별을 userNum(숫자)에서 userId(문자열)로 바꿨다. 응답에 없는 필드는 null.
    val userNum: Long? = null,
    val userId: String? = null,
    val nickname: String? = null
)
@Keep
data class UserStatsResponse(
    val code: Int,
    val message: String,
    val userStats: List<UserStats>
)
@Keep
data class UserStats(
    val seasonId: Int,
    val userNum: Int,
    val matchingMode: Int,
    val matchingTeamMode: Int,
    val mmr: Int,
    val nickname: String,
    val rank: Int,
    val rankSize: Int,
    val totalGames: Long,
    val totalWins: Long,
    val totalTeamKills: Int,
    val rankPercent: Float,
    val averageRank: Float,
    val averageKills: Float,
    val averageAssistants: Float,
    val averageHunts: Float,
    val top1: Float,
    val top2: Float,
    val top3: Float,
    val top5: Float,
    val top7: Float,
    val characterStats: List<CharacterStats>
)
@Keep
data class CharacterStats(
    val characterCode: Int,
    val totalGames: Long?,
    val usages: Long,
    val maxKillings: Int,
    val top3: Int,
    val wins: Int,
    val top3Rate: Float,
    val averageRank: Float
)

/** 시즌 목록 응답 (GET v2/data/Season). 필드가 바뀌어도 앱이 죽지 않도록 모두 nullable. */
@Keep
data class SeasonResponse(
    val code: Int?,
    val message: String?,
    val data: List<SeasonData>?
)

@Keep
data class SeasonData(
    val seasonID: Int?,
    val seasonName: String?,
    val isCurrent: Int?
)


/** 채널 업로드 목록 응답 (GET playlistItems). 호출당 한도 1. */
@Keep
data class PlaylistItemsResponse(
    @SerializedName("items")
    val items: List<PlaylistItem>? = null
)

@Keep
data class PlaylistItem(
    @SerializedName("snippet")
    val snippet: PlaylistSnippet? = null
)

@Keep
data class PlaylistSnippet(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("thumbnails")
    val thumbnails: ThumbNail? = null,
    @SerializedName("resourceId")
    val resourceId: Id? = null
)
