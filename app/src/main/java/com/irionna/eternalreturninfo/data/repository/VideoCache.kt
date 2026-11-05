package com.irionna.eternalreturninfo.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.irionna.eternalreturninfo.data.model.VideoModel

/**
 * 홈의 "게임 방송" 영상 목록을 이 기기에 저장해 두고 [TTL_MS](6시간) 동안은 YouTube 를 다시 부르지 않는다.
 * (YouTube 검색은 호출 하나가 하루 한도의 100 을 쓰므로, 앱을 열 때마다 부르면 한도가 금방 바닥난다)
 * 새로 받기에 실패하면 6시간이 지난 저장본이라도 보여준다.
 */
object VideoCache {
    const val TTL_MS = 6 * 60 * 60 * 1000L
    private const val PREFS = "video_cache"
    private const val KEY_JSON = "list"
    private const val KEY_TIME = "saved_at"

    class Saved(val list: List<VideoModel>, val savedAt: Long) {
        fun isFresh(now: Long = System.currentTimeMillis()) = now - savedAt in 0 until TTL_MS
    }

    fun load(context: Context): Saved? = try {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = p.getString(KEY_JSON, null)
        val list: List<VideoModel>? = json?.let {
            Gson().fromJson(it, object : TypeToken<List<VideoModel>>() {}.type)
        }
        if (list.isNullOrEmpty()) null else Saved(list, p.getLong(KEY_TIME, 0L))
    } catch (e: Exception) {
        null
    }

    fun save(context: Context, list: List<VideoModel>) {
        if (list.isEmpty()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_JSON, Gson().toJson(list))
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }
}
