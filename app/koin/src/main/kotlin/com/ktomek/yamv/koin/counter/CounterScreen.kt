package com.ktomek.yamv.koin.counter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ktomek.yamv.ui.counter.view.CounterContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun CounterScreen(
    viewModel: CounterViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    CounterContent(state = state, onDispatch = viewModel::dispatch)
}
