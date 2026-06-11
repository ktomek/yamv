package com.ktomek.yamv.koin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.ktomek.yamv.koin.counter.CounterScreen
import com.ktomek.yamv.koin.counter.counterModule
import com.ktomek.yamv.ui.theme.YamvTheme
import kotlinx.browser.document
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(counterModule)
    }
    ComposeViewport(document.body!!) {
        YamvTheme {
            CounterScreen()
        }
    }
}
