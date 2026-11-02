package com.irionna.eternalreturninfo

import com.irionna.eternalreturninfo.util.NicknameKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NicknameKeyTest {
    @Test
    fun trimsAndLowercases() {
        assertEquals("abc", NicknameKey.normalize("  AbC "))
    }

    @Test
    fun keepsKorean() {
        assertEquals("이리온나", NicknameKey.normalize("이리온나"))
    }

    @Test
    fun sameKeyIgnoringCase() {
        assertEquals(NicknameKey.normalize("Hosik"), NicknameKey.normalize("hOSIK"))
    }

    @Test
    fun rejectsInvalid() {
        assertNull(NicknameKey.normalize(null))
        assertNull(NicknameKey.normalize("   "))
        assertNull(NicknameKey.normalize("a/b"))
        assertNull(NicknameKey.normalize("."))
        assertNull(NicknameKey.normalize(".."))
        assertNull(NicknameKey.normalize("__id__"))
        assertNull(NicknameKey.normalize("a".repeat(101)))
    }
}
