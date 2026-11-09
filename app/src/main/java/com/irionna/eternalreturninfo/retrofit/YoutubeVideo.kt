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
    val snippet: PlaylistSnippet? = null,
    @SerializedName("contentDetails")
    val contentDetails: PlaylistContentDetails? = null
)

@Keep
data class PlaylistContentDetails(
    @SerializedName("videoPublishedAt")
    val videoPublishedAt: String? = null
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


/** 영상 정보 응답 (GET videos). 검색 결과에 없는 태그를 얻는 데 쓴다. 호출당 한도 1 (영상 50개까지). */
@Keep
data class VideoDetailsResponse(
    @SerializedName("items")
    val items: List<VideoDetail>? = null
)

@Keep
data class VideoDetail(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("snippet")
    val snippet: VideoDetailSnippet? = null,
    @SerializedName("contentDetails")
    val contentDetails: VideoDetailContent? = null
)

/** 영상 길이 (ISO 8601, 예: PT1M5S). 쇼츠 구분에 쓴다. */
@Keep
data class VideoDetailContent(
    @SerializedName("duration")
    val duration: String? = null
)

@Keep
data class VideoDetailSnippet(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("tags")
    val tags: List<String>? = null,
    @SerializedName("thumbnails")
    val thumbnails: ThumbNail? = null
)


/** 무료 실험체 응답 (GET v1/freeCharacters/{matchingMode}). 일반(2)만 목록이 오고 랭크(3)는 빈 목록. 번호 = 실험체 번호. */
@Keep
data class FreeCharactersResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("freeCharacters")
    val freeCharacters: List<Int>? = null
)


/** 경기 기록 응답 (GET v1/user/games/uid/{userId}). 최근 경기가 먼저, next 가 있으면 더 불러올 수 있다. 모든 필드는 없을 수 있다. */
@Keep
data class UserGamesResponse(
    @SerializedName("code")
    val code: Int? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("userGames")
    val userGames: List<UserGame>? = null,
    @SerializedName("next")
    val next: Long? = null
)

@Keep
data class UserGame(
    @SerializedName("gameId")
    val gameId: Long? = null,
    @SerializedName("seasonId")
    val seasonId: Int? = null,
    @SerializedName("matchingMode")
    val matchingMode: Int? = null,
    @SerializedName("matchingTeamMode")
    val matchingTeamMode: Int? = null,
    @SerializedName("characterNum")
    val characterNum: Int? = null,
    @SerializedName("gameRank")
    val gameRank: Int? = null,
    @SerializedName("playerKill")
    val playerKill: Int? = null,
    @SerializedName("playerAssistant")
    val playerAssistant: Int? = null,
    @SerializedName("damageToPlayer")
    val damageToPlayer: Int? = null,
    @SerializedName("startDtm")
    val startDtm: String? = null,
    @SerializedName("duration")
    val duration: Int? = null
)


/** 스트리머 영상 목록 파일(tools/fetch_videos.py 가 만든 JSON). 필드가 빠져도 앱이 죽지 않도록 모두 nullable. */
@Keep
data class VideoFeed(
    @SerializedName("generatedAt")
    val generatedAt: Long? = null,
    @SerializedName("items")
    val items: List<VideoFeedItem>? = null
)

@Keep
data class VideoFeedItem(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("thumbnail")
    val thumbnail: String? = null,
    @SerializedName("isShort")
    val isShort: Boolean? = null
)
