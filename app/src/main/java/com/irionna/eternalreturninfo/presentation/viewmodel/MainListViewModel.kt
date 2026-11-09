package com.irionna.eternalreturninfo.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.irionna.eternalreturninfo.data.model.Notice
import com.irionna.eternalreturninfo.data.model.VideoModel
import com.irionna.eternalreturninfo.data.repository.FreeCharactersCache
import com.irionna.eternalreturninfo.data.repository.GameStats
import com.irionna.eternalreturninfo.data.repository.VideoCache
import com.irionna.eternalreturninfo.retrofit.RetrofitInstance
import com.irionna.eternalreturninfo.util.Constants
import com.irionna.eternalreturninfo.util.VideoFilter
import com.irionna.eternalreturninfo.util.VideoKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainListViewModel() : ViewModel(){

    private companion object {
        const val MAX_STREAMER_VIDEOS = 20
    }


    private val _noticeList = MutableLiveData<MutableList<Notice>>()
    val noticeList: LiveData<MutableList<Notice>>
        get() = _noticeList

    private val _videoList = MutableLiveData<MutableList<VideoModel>>()
    val videoList: LiveData<MutableList<VideoModel>>
        get() = _videoList

    /** 스트리머 탭 결과: (정렬 종류, 영상 목록) */
    private val _streamerList = MutableLiveData<Pair<String, List<VideoModel>>>()
    val streamerList: LiveData<Pair<String, List<VideoModel>>>
        get() = _streamerList



    /** 이번에 무료인 실험체 번호 (비어 있으면 줄을 숨긴다) */
    private val _freeCharacters = MutableLiveData<List<Int>>()
    val freeCharacters: LiveData<List<Int>>
        get() = _freeCharacters

    private var freeRequested = false

    /** 무료 실험체를 불러온다. 6시간 저장이 있으면 그대로 쓰고, [force] 면 새로 받는다. 실패하면 오래된 저장본. */
    fun loadFreeCharacters(context: Context, force: Boolean = false) {
        if (freeRequested && !force) return
        freeRequested = true
        val appContext = context.applicationContext
        viewModelScope.launch {
            val saved = FreeCharactersCache.load(appContext)
            if (!force && saved != null && saved.isFresh()) {
                _freeCharacters.postValue(saved.list)
                return@launch
            }
            val fresh = GameStats.freeCharacters()
            if (!fresh.isNullOrEmpty()) {
                FreeCharactersCache.save(appContext, fresh)
                _freeCharacters.postValue(fresh)
            } else {
                _freeCharacters.postValue(saved?.list ?: emptyList())
            }
        }
    }

    /** 공지를 받지 못했을 때 true (배너 자리에 안내 문구를 보여준다) */
    private val _noticeFailed = MutableLiveData<Boolean>()
    val noticeFailed: LiveData<Boolean>
        get() = _noticeFailed

    init {
        loadNotices()
    }

    /** 공식 사이트 공지 배너를 새로 받는다. 화면 수명이 아니라 ViewModel 수명에 묶여서, ViewModel 이 사라지면 같이 취소된다. */
    fun loadNotices() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { fetchNotices() }
            if (list != null) {
                _noticeList.postValue(list.toMutableList())
                _noticeFailed.postValue(list.isEmpty())
            } else {
                _noticeFailed.postValue(true)
            }
        }
    }

    private fun fetchNotices(): List<Notice>? = try {
        val response = RetrofitInstance.eternalApi.getNews("locale=ko_KR").execute()
        if (response.isSuccessful) {
            response.body()?.articles?.map {
                Notice(it.thumbnail_url, it.i18ns["ko_KR"]?.title, it.i18ns["ko_KR"]?.summary, it.i18ns["ko_KR"]?.created_at_for_humans, it.url)
            }
        } else {
            Log.d("공지 api 응답 오류", response.message())
            null
        }
    } catch (e: Exception) {
        Log.d("공지", "공지 오류: ${e.javaClass.simpleName}")
        null
    }

    /** 아래로 당겨서 새로고침: 공지와 공식 영상을 다시 받는다. 스트리머 검색은 한도 때문에 6시간 저장을 그대로 따른다. */
    fun refreshHome(context: Context) {
        loadNotices()
        videosRequested = false
        loadVideos(context, force = true)
        loadFreeCharacters(context, force = true)
    }

    private var videosRequested = false

    /**
     * 게임 방송 영상 목록. 이 기기에 저장된 목록이 6시간 이내면 그대로 쓰고, 아니면 YouTube 에서 새로 받는다.
     * 새로 받기에 실패하면 오래된 저장본이라도 보여준다. 화면이 다시 만들어져도 한 번만 요청한다.
     */
    fun loadVideos(context: Context, force: Boolean = false) {
        if (videosRequested) return
        videosRequested = true
        val appContext = context.applicationContext
        viewModelScope.launch {
            val saved = VideoCache.load(appContext, VideoCache.KIND_OFFICIAL)
            if (!force && saved != null && saved.isFresh()) {
                _videoList.postValue(saved.list.toMutableList())
                return@launch
            }
            val fresh = fetchItemResults()
            if (!fresh.isNullOrEmpty()) {
                VideoCache.save(appContext, VideoCache.KIND_OFFICIAL, fresh)
                _videoList.postValue(fresh.toMutableList())
            } else {
                _videoList.postValue((saved?.list ?: emptyList()).toMutableList())
            }
        }
    }

    private val streamersRequested = mutableSetOf<String>()

    /**
     * 스트리머 탭: YouTube 검색 결과 중 이터널 리턴 관련 영상만 보여준다. [sort] = "date"(최신순) 또는 "viewCount"(조회순, 최근 30일).
     * 정렬마다 6시간 저장하고, 처음 열 때 한 번만 불러온다. 검색은 호출당 한도 100 이라 새로 받기는 정렬당 6시간에 한 번뿐이다.
     */
    fun loadStreamerVideos(context: Context, sort: String) {
        if (!streamersRequested.add(sort)) return
        val appContext = context.applicationContext
        val kind = "streamer_$sort"
        viewModelScope.launch {
            val saved = VideoCache.load(appContext, kind)
            if (saved != null && saved.isFresh()) {
                _streamerList.postValue(sort to saved.list)
                return@launch
            }
            val fresh = fetchStreamerVideos(sort)
            if (!fresh.isNullOrEmpty()) {
                VideoCache.save(appContext, kind, fresh)
                _streamerList.postValue(sort to fresh)
            } else {
                _streamerList.postValue(sort to (saved?.list ?: emptyList()))
            }
        }
    }

    /** 검색 → 영상 정보(태그) 조회 → 키워드로 거르기. 검색·조회가 실패하면 null. */
    private suspend fun fetchStreamerVideos(sort: String): List<VideoModel>? {
        return try {
            // 조회순은 오래된 인기 영상만 나오지 않도록 최근 30일로 제한한다
            val after = if (sort == "viewCount") {
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
                fmt.format(java.util.Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))
            } else null
            val search = RetrofitInstance.api.searchVideos(query = VideoFilter.SEARCH_QUERY, order = sort, publishedAfter = after)
            if (!search.isSuccessful) {
                Log.e("YouTube", "스트리머 검색 실패 HTTP ${search.code()}: ${search.errorBody()?.string()?.take(300)}")
                return null
            }
            val ids = search.body()?.items.orEmpty().mapNotNull { it.id.videoId }
            if (ids.isEmpty()) return emptyList()

            val details = RetrofitInstance.api.getVideoDetails(ids = ids.joinToString(","))
            if (!details.isSuccessful) {
                Log.e("YouTube", "영상 정보 실패 HTTP ${details.code()}: ${details.errorBody()?.string()?.take(300)}")
                return null
            }
            val byId = details.body()?.items.orEmpty().filter { it.id != null }.associateBy { it.id!! }
            val result = ids.mapNotNull { id ->
                val d = byId[id] ?: return@mapNotNull null
                val sn = d.snippet ?: return@mapNotNull null
                if (!VideoFilter.matches(sn.title, sn.tags)) return@mapNotNull null
                VideoModel(
                    id = id,
                    title = sn.title,
                    thumbnail = sn.thumbnails?.medium?.url,
                    url = "https://www.youtube.com/watch?v=$id",
                    isShort = VideoKind.isShort(d.contentDetails?.duration, sn.title, sn.tags)
                )
            }.take(MAX_STREAMER_VIDEOS)
            Log.d("YouTube", "스트리머($sort) 검색 ${ids.size}개 중 ${result.size}개 통과")
            result
        } catch (e: Exception) {
            Log.e("YouTube", "스트리머 목록 오류: ${e.javaClass.simpleName} ${e.message}")
            null
        }
    }

    /** 새로 받은 영상 목록(공식 채널 최신 업로드). 실패하면 null. */
    private suspend fun fetchItemResults(): List<VideoModel>? {
        return try {
            val response = RetrofitInstance.api.getPlaylistVideos(
                playlistId = Constants.OFFICIAL_UPLOADS_PLAYLIST,
                maxResults = 10
            )
            if (!response.isSuccessful) {
                // 실패 이유(키 제한, 한도 초과, API 미사용 등)를 로그로 남긴다. 키는 포함되지 않는다.
                Log.e("YouTube", "목록 실패 HTTP ${response.code()}: ${response.errorBody()?.string()?.take(400)}")
                return null
            }
            val items = response.body()?.items.orEmpty()
            Log.d("YouTube", "공식 채널 목록 성공: 영상 ${items.size}개")
            // 쇼츠 구분용 길이: 영상 정보를 한 번 더 받는다 (호출당 한도 1). 실패하면 모두 일반 영상으로 본다.
            val details = try {
                val ids = items.mapNotNull { it.snippet?.resourceId?.videoId }
                if (ids.isEmpty()) emptyMap() else RetrofitInstance.api.getVideoDetails(ids = ids.joinToString(","))
                    .body()?.items.orEmpty().filter { it.id != null }.associateBy { it.id!! }
            } catch (e: Exception) {
                Log.w("YouTube", "공식 영상 길이 조회 실패: ${e.javaClass.simpleName}")
                emptyMap()
            }
            items.mapNotNull { item ->
                val sn = item.snippet ?: return@mapNotNull null
                val videoId = sn.resourceId?.videoId ?: return@mapNotNull null
                val title = sn.title
                // 삭제됐거나 비공개로 바뀐 영상은 제목이 이렇게 온다
                if (title == "Private video" || title == "Deleted video") return@mapNotNull null
                val d = details[videoId]
                VideoModel(
                    id = videoId,
                    title = title,
                    thumbnail = sn.thumbnails?.medium?.url,
                    url = "https://www.youtube.com/watch?v=$videoId",
                    isShort = VideoKind.isShort(d?.contentDetails?.duration, title, d?.snippet?.tags)
                )
            }
        } catch (e: Exception) {
            Log.e("YouTube", "목록 오류: ${e.javaClass.simpleName} ${e.message}")
            null
        }
    }

}

class MainListViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainListViewModel::class.java)) {
            return MainListViewModel() as T
        } else {
            throw IllegalArgumentException("Not found ViewModel class.")
        }
    }
}
