package com.ktomek.yamv.koin

import androidx.compose.ui.window.ComposeUIViewController
import com.ktomek.yamv.koin.counter.CounterScreen
import com.ktomek.yamv.koin.counter.counterModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(counterModule)
    }
}

@Suppress("FunctionNaming")
fun MainViewController() = ComposeUIViewController { CounterScreen() }
