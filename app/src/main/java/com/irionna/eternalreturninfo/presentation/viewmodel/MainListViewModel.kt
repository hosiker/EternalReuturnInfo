package com.irionna.eternalreturninfo.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.irionna.eternalreturninfo.data.model.Notice
import com.irionna.eternalreturninfo.data.model.VideoModel
import com.irionna.eternalreturninfo.retrofit.BoardSingletone
import com.irionna.eternalreturninfo.retrofit.RetrofitInstance
import com.irionna.eternalreturninfo.retrofit.UserStats
import com.irionna.eternalreturninfo.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainListViewModel() : ViewModel(){

    private val _noticeList = MutableLiveData<MutableList<Notice>>()
    val noticeList: LiveData<MutableList<Notice>>
        get() = _noticeList

    private val _videoList = MutableLiveData<MutableList<VideoModel>>()
    val videoList: LiveData<MutableList<VideoModel>>
        get() = _videoList

    private val _userRecordList = MutableLiveData<UserStats?>()
    val userRecordList: LiveData<UserStats?>
        get() = _userRecordList



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


                //전적 검색
                val nickname = fetchNonNullableNickname()

                val userIDCall = RetrofitInstance.searchUserIDApi.getUserByNickname(Constants.MAIN_APIKEY, nickname)
                val userIDResponse = userIDCall.execute()

                if (userIDResponse.isSuccessful) {
                    val gameResponse = userIDResponse.body()

                    if(gameResponse != null){
                        val userNum = gameResponse?.user?.userNum.toString()
                        val seasonId = BoardSingletone.seasonID()

                        val userStateCall = RetrofitInstance.searchUserStateApi.getUserStats(
                            Constants.MAIN_APIKEY, userNum, seasonId)
                        val userStateResponse = userStateCall.execute()

                        if (userStateResponse.isSuccessful) {
                            val userState = userStateResponse.body()

                            if(userState != null){
                                // 점검용: 서버가 시즌·모드별로 몇 개의 기록을 주는지 확인한다 (키는 남기지 않음)
                                Log.d("ERStats", "seasonId(요청)=$seasonId, 기록 ${userState.userStats.size}개")
                                userState.userStats.forEach { st ->
                                    Log.d(
                                        "ERStats",
                                        "season=${st.seasonId} mode=${st.matchingMode} team=${st.matchingTeamMode} " +
                                            "mmr=${st.mmr} rank=${st.rank} games=${st.totalGames} wins=${st.totalWins}"
                                    )
                                }
                                val user = userState?.userStats?.get(0)
                                _userRecordList.postValue(user)
                            }

                        } else {
                            Log.d("userStateResponse", "${userStateResponse}")
                        }
                    }

                }

            }catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch {
            fetchItemResults()
        }


    }

    private suspend fun fetchItemResults() {

        val resItems: ArrayList<VideoModel> = ArrayList()

        try {
            val query = "이터널리턴"

            val response = RetrofitInstance.api.getYouTubeVideos(
                query = query,
                maxResults = 10,
                videoOrder = "relevance"
            )

            resItems.clear()

            if (response.isSuccessful) {
                val youtubeVideo = response.body()!!

                if(youtubeVideo != null){
                    youtubeVideo?.items?.forEach { snippet ->
                        val title = snippet.snippet.title
                        val url = snippet.snippet.thumbnails.medium.url
                        resItems.add(VideoModel(id= snippet.id.videoId, title = title, thumbnail = url,  url = "https://www.youtube.com/watch?v=${snippet.id}"))
                    }
                }

            }else{
            }

            _videoList.postValue(resItems.toMutableList())

        } catch (e: Exception) {
            Log.e("#error check", "Error: ${e.message}")
        }
    }

    private suspend fun fetchNonNullableNickname(): String {
        var nickname: String? = null
        while (nickname == null) {
            nickname = BoardSingletone.LoginUser().name
            if (nickname == null) {
                // 닉네임이 null인 경우 잠시 대기하거나 다른 작업 수행
                delay(1000)
            }
        }
        return nickname
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
