package com.ktomek.yamv.intention

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.MviExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

interface IntentionRouter<S : State> {
    fun observeOutcomes(): SharedFlow<Outcome<S>>
    suspend fun dispatchIntention(intention: Any)
    fun initialize(
        scope: CoroutineScope,
        dispatcherConfig: CoroutineDispatcherConfig,
        exceptionHandler: MviExceptionHandler = MviExceptionHandler.Default,
    )
}
