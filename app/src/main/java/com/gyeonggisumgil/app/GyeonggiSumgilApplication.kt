package com.gyeonggisumgil.app

import android.app.Application
import android.util.Log

class GyeonggiSumgilApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Gyeonggi Sumgil app started")
    }

    private companion object {
        const val TAG = "GyeonggiSumgil"
    }
}
