package com.ktomek.yamv.counter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ktomek.yamv.hilt.hiltMviStore
import com.ktomek.yamv.ui.counter.view.CounterContent

@Composable
fun CounterScreen(
    store: CounterStateStore = hiltMviStore()
) {
    val state by store.state.collectAsState()
    CounterContent(state = state, onDispatch = store::dispatch)
}
