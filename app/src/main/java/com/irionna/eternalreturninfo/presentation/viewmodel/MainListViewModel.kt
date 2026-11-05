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

    private val _noticeList = MutableLiveData<MutableList<Notice>>()
    val noticeList: LiveData<MutableList<Notice>>
        get() = _noticeList

    private val _videoList = MutableLiveData<MutableList<VideoModel>>()
    val videoList: LiveData<MutableList<VideoModel>>
        get() = _videoList



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
            val saved = VideoCache.load(appContext)
            if (saved != null && saved.isFresh()) {
                _videoList.postValue(saved.list.toMutableList())
                return@launch
            }
            val fresh = fetchItemResults()
            if (!fresh.isNullOrEmpty()) {
                VideoCache.save(appContext, fresh)
                _videoList.postValue(fresh.toMutableList())
            } else {
                _videoList.postValue((saved?.list ?: emptyList()).toMutableList())
            }
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
