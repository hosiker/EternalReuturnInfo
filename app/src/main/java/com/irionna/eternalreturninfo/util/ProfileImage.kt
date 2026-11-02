package com.irionna.eternalreturninfo.util

import android.content.Context
import android.widget.ImageView
import com.irionna.eternalreturninfo.R

/**
 * 프로필 이미지는 이용자 사진이 아니라 선호 실험체 이미지(앱에 내장된 drawable)를 쓴다.
 * DB 에는 "res:character_5" 처럼 drawable 이름을 가리키는 값만 저장하고,
 * 값이 없거나 옛 형식(Storage 사진 주소 등)이면 기본 이미지를 보여준다.
 *
 * 나중에 이용자 사진 업로드를 다시 지원하면(Blaze 전환 등) 값 형식에 주소를 허용하면 된다.
 */
object ProfileImage {
    const val RES_PREFIX = "res:"
    const val DEFAULT_KEY = "res:ic_baseimage"

    /** characterName 배열의 위치(0 = "선택")에 해당하는 저장값. 선택하지 않았으면 기본 이미지. */
    fun keyForCharacterIndex(index: Int): String =
        if (index >= 1) "${RES_PREFIX}character_$index" else DEFAULT_KEY

    fun keyForCharacter(name: String?, characterNames: Array<String>): String =
        keyForCharacterIndex(characterNames.indexOf(name))

    /** 저장값에서 drawable 이름을 꺼낸다. 형식이 맞지 않으면 null. */
    fun drawableNameOf(value: String?): String? =
        value?.takeIf { it.startsWith(RES_PREFIX) }?.removePrefix(RES_PREFIX)?.takeIf { it.isNotBlank() }

    fun resIdOf(context: Context, value: String?): Int {
        val name = drawableNameOf(value) ?: return R.drawable.ic_baseimage
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        return if (id != 0) id else R.drawable.ic_baseimage
    }

    fun load(imageView: ImageView, value: String?) {
        imageView.setImageResource(resIdOf(imageView.context, value))
    }
}
