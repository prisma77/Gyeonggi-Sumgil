package com.gyeonggisumgil.app

import android.app.Application
import android.os.Build
import android.util.Log
import com.kakao.vectormap.KakaoMapSdk

class GyeonggiSumgilApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (!MapRuntime.isKakaoMapSupported()) {
            Log.w(TAG, "Kakao Maps SDK skipped on unsupported ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            return
        }

        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
            Log.w(TAG, "KAKAO_NATIVE_APP_KEY is missing")
            return
        }

        KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
    }

    private companion object {
        const val TAG = "GyeonggiSumgil"
    }
}
