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
import com.irionna.eternalreturninfo.data.repository.VideoCache
import com.irionna.eternalreturninfo.retrofit.RetrofitInstance
import com.irionna.eternalreturninfo.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainListViewModel() : ViewModel(){

    private companion object {
        const val PER_STREAMER = 5
        const val MAX_STREAMER_VIDEOS = 20
    }


    private val _noticeList = MutableLiveData<MutableList<Notice>>()
    val noticeList: LiveData<MutableList<Notice>>
        get() = _noticeList

    private val _videoList = MutableLiveData<MutableList<VideoModel>>()
    val videoList: LiveData<MutableList<VideoModel>>
        get() = _videoList

    private val _streamerList = MutableLiveData<MutableList<VideoModel>>()
    val streamerList: LiveData<MutableList<VideoModel>>
        get() = _streamerList



    init {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val call = RetrofitInstance.eternalApi.getNews("locale=ko_KR")
                val response = call.execute()

                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if (responseBody != null) {
                        val articles = responseBody.articles

                        val list = mutableListOf<Notice>()
                        articles.mapNotNull { list.add(Notice(it.thumbnail_url, it.i18ns["ko_KR"]?.title, it.i18ns["ko_KR"]?.summary, it.i18ns["ko_KR"]?.created_at_for_humans, it.url))}

                        _noticeList.postValue(list)

                    }
                } else {
                    Log.d("공지 api 응답 오류", response.message())
                }



            }catch (e: Exception) {
                e.printStackTrace()
            }
        }



    }

    private var videosRequested = false

    /**
     * 게임 방송 영상 목록. 이 기기에 저장된 목록이 6시간 이내면 그대로 쓰고, 아니면 YouTube 에서 새로 받는다.
     * 새로 받기에 실패하면 오래된 저장본이라도 보여준다. 화면이 다시 만들어져도 한 번만 요청한다.
     */
    fun loadVideos(context: Context) {
        if (videosRequested) return
        videosRequested = true
        val appContext = context.applicationContext
        viewModelScope.launch {
            val saved = VideoCache.load(appContext, VideoCache.KIND_OFFICIAL)
            if (saved != null && saved.isFresh()) {
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

    private var streamersRequested = false

    /**
     * 스트리머 탭: 등록한 채널들의 최근 영상을 모아 최신순으로 보여준다. 탭을 처음 열 때 한 번만 불러온다.
     * 저장·실패 처리는 공식 채널 목록과 같다 (6시간 이내면 저장본, 실패하면 오래된 저장본).
     */
    fun loadStreamerVideos(context: Context) {
        if (streamersRequested) return
        streamersRequested = true
        val appContext = context.applicationContext
        viewModelScope.launch {
            val saved = VideoCache.load(appContext, VideoCache.KIND_STREAMER)
            if (saved != null && saved.isFresh()) {
                _streamerList.postValue(saved.list.toMutableList())
                return@launch
            }
            val fresh = fetchStreamerVideos()
            if (!fresh.isNullOrEmpty()) {
                VideoCache.save(appContext, VideoCache.KIND_STREAMER, fresh)
                _streamerList.postValue(fresh.toMutableList())
            } else {
                _streamerList.postValue((saved?.list ?: emptyList()).toMutableList())
            }
        }
    }

    /** 채널마다 최근 영상 몇 개를 받아 합친 뒤 최신순으로 정렬한다. 채널 하나가 실패해도 나머지는 보여준다. 전부 실패하면 null. */
    private suspend fun fetchStreamerVideos(): List<VideoModel>? {
        val all = mutableListOf<Pair<String, VideoModel>>() // (게시 시각, 영상)
        var anySuccess = false
        for (channelId in Constants.STREAMER_CHANNEL_IDS) {
            try {
                val response = RetrofitInstance.api.getPlaylistVideos(
                    playlistId = Constants.uploadsPlaylistOf(channelId),
                    maxResults = PER_STREAMER
                )
                if (!response.isSuccessful) {
                    Log.e("YouTube", "스트리머 목록 실패 HTTP ${response.code()}: ${response.errorBody()?.string()?.take(200)}")
                    continue
                }
                anySuccess = true
                response.body()?.items.orEmpty().forEach { item ->
                    val sn = item.snippet ?: return@forEach
                    val videoId = sn.resourceId?.videoId ?: return@forEach
                    val title = sn.title
                    if (title == "Private video" || title == "Deleted video") return@forEach
                    val at = item.contentDetails?.videoPublishedAt ?: ""
                    all.add(
                        at to VideoModel(
                            id = videoId,
                            title = title,
                            thumbnail = sn.thumbnails?.medium?.url,
                            url = "https://www.youtube.com/watch?v=$videoId"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("YouTube", "스트리머 목록 오류: ${e.javaClass.simpleName} ${e.message}")
            }
        }
        if (!anySuccess) return null
        Log.d("YouTube", "스트리머 영상 ${all.size}개")
        return all.sortedByDescending { it.first }.map { it.second }.distinctBy { it.id }.take(MAX_STREAMER_VIDEOS)
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
            items.mapNotNull { item ->
                val sn = item.snippet ?: return@mapNotNull null
                val videoId = sn.resourceId?.videoId ?: return@mapNotNull null
                val title = sn.title
                // 삭제됐거나 비공개로 바뀐 영상은 제목이 이렇게 온다
                if (title == "Private video" || title == "Deleted video") return@mapNotNull null
                VideoModel(
                    id = videoId,
                    title = title,
                    thumbnail = sn.thumbnails?.medium?.url,
                    url = "https://www.youtube.com/watch?v=$videoId"
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
