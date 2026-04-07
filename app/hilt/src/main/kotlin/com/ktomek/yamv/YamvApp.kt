package com.ktomek.yamv

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class YamvApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initLogging()
    }

    private fun initLogging() {
        Timber.uprootAll()
        Timber.plant(Timber.DebugTree())
    }
}
