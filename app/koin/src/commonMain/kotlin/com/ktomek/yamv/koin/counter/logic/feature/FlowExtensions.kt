package com.ktomek.yamv.koin.counter.logic.feature

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun <T> Flow<T>.takeUntilSignal(signal: Flow<*>): Flow<T> = channelFlow {
    val job = launch { collect { send(it) } }
    signal.first()
    job.cancel()
}
