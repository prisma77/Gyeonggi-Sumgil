package com.gyeonggisumgil.app

import android.os.Build

object MapRuntime {
    fun isKakaoMapSupported(): Boolean = isKakaoMapSupported(Build.SUPPORTED_ABIS ?: emptyArray())

    internal fun isKakaoMapSupported(supportedAbis: Array<String>): Boolean {
        val primaryAbi = supportedAbis.firstOrNull().orEmpty()
        return primaryAbi == "arm64-v8a" || primaryAbi == "armeabi-v7a"
    }
}
