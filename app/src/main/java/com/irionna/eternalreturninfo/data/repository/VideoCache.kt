package com.irionna.eternalreturninfo.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.irionna.eternalreturninfo.data.model.VideoModel

/**
 * 홈의 "게임 방송" 영상 목록을 이 기기에 저장해 두고 [TTL_MS](6시간) 동안은 YouTube 를 다시 부르지 않는다.
 * (공식 채널 업로드 목록은 호출당 한도 1 이지만, 앱을 열 때마다 부를 필요는 없다. 저장 키에 버전을 붙여 출처가 바뀌면 옛 목록은 쓰지 않는다)
 * 새로 받기에 실패하면 6시간이 지난 저장본이라도 보여준다.
 */
object VideoCache {
    const val TTL_MS = 6 * 60 * 60 * 1000L
    private const val PREFS = "video_cache"
    private fun keyJson(kind: String) = "list_${kind}_v2"
    private fun keyTime(kind: String) = "saved_at_${kind}_v2"

    const val KIND_OFFICIAL = "official"
    const val KIND_STREAMER = "streamer"

    class Saved(val list: List<VideoModel>, val savedAt: Long) {
        fun isFresh(now: Long = System.currentTimeMillis()) = now - savedAt in 0 until TTL_MS
    }

    fun load(context: Context, kind: String): Saved? = try {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = p.getString(keyJson(kind), null)
        val list: List<VideoModel>? = json?.let {
            Gson().fromJson(it, object : TypeToken<List<VideoModel>>() {}.type)
        }
        if (list.isNullOrEmpty()) null else Saved(list, p.getLong(keyTime(kind), 0L))
    } catch (e: Exception) {
        null
    }

    fun save(context: Context, kind: String, list: List<VideoModel>) {
        if (list.isEmpty()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(keyJson(kind), Gson().toJson(list))
            .putLong(keyTime(kind), System.currentTimeMillis())
            .apply()
    }
}
