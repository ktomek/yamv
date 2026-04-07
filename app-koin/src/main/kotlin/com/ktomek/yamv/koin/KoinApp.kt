package com.ktomek.yamv.koin

import android.app.Application
import com.ktomek.yamv.koin.ui.counter.CounterViewModel
import com.ktomek.yamv.koin.ui.counter.logic.state.counterStateFeaturesKoinModule
import com.ktomek.yamv.koin.ui.counter.logic.state.counterStateKoinModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

class KoinApp : Application() {

    private val viewModelModule = module {
        viewModel { CounterViewModel(get()) }
    }

    override fun onCreate() {
        super.onCreate()
        initLogging()
        startKoin {
            androidContext(this@KoinApp)
            modules(counterStateFeaturesKoinModule, counterStateKoinModule, viewModelModule)
        }
    }

    private fun initLogging() {
        Timber.uprootAll()
        Timber.plant(Timber.DebugTree())
    }
}
