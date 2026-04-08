package com.ktomek.yamv.koin

import android.app.Application
import com.ktomek.yamv.koin.counter.counterModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class KoinApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initLogging()
        startKoin {
            androidContext(this@KoinApp)
            modules(counterModule)
        }
    }

    private fun initLogging() {
        Timber.uprootAll()
        Timber.plant(Timber.DebugTree())
    }
}
