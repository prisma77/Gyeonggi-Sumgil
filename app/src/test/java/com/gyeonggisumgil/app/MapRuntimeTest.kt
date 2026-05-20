package com.gyeonggisumgil.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapRuntimeTest {
    @Test
    fun arm64PrimaryAbiSupportsKakaoMap() {
        assertTrue(MapRuntime.isKakaoMapSupported(arrayOf("arm64-v8a")))
    }

    @Test
    fun x86PrimaryAbiSkipsKakaoMap() {
        assertFalse(MapRuntime.isKakaoMapSupported(arrayOf("x86_64", "arm64-v8a")))
    }
}
