package com.ktomek.yamv.intention

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.state.YamvDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

/**
 * MVI dispatcher responsible for processing intentions and producing outcomes.
 *
 * @param S The type of the state for outcomes.
 */
internal interface IntentionDispatcher<S : State> {

    /**
     * Listens for intentions from upstream and processes them.
     *
     * @return A [SharedFlow] emitting the outcomes produced by the dispatcher.
     */
    fun observeOutcomes(): SharedFlow<Outcome<S>>

    /**
     * Processes a single intention.
     *
     * @param intention The intention to be processed.
     */
    suspend fun dispatchIntention(intention: Any)
    fun initialize(scope: CoroutineScope, dispatcher: YamvDispatcherProvider)
}
