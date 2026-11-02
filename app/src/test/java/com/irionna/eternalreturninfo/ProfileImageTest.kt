package com.irionna.eternalreturninfo

import com.irionna.eternalreturninfo.util.ProfileImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileImageTest {
    private val names = arrayOf("선택", "재키", "아야", "현우")

    @Test
    fun characterMapsToDrawableKey() {
        assertEquals("res:character_2", ProfileImage.keyForCharacter("아야", names))
    }

    @Test
    fun unselectedOrUnknownCharacterUsesDefault() {
        assertEquals(ProfileImage.DEFAULT_KEY, ProfileImage.keyForCharacter("선택", names))
        assertEquals(ProfileImage.DEFAULT_KEY, ProfileImage.keyForCharacter("없는이름", names))
        assertEquals(ProfileImage.DEFAULT_KEY, ProfileImage.keyForCharacter(null, names))
    }

    @Test
    fun drawableNameParsing() {
        assertEquals("character_5", ProfileImage.drawableNameOf("res:character_5"))
        assertNull(ProfileImage.drawableNameOf("https://firebasestorage.googleapis.com/x.jpg"))
        assertNull(ProfileImage.drawableNameOf("res:"))
        assertNull(ProfileImage.drawableNameOf(null))
        assertNull(ProfileImage.drawableNameOf(""))
    }
}
