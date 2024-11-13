package com.ktomek.yamv.intention

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.SharedFlow
import java.io.Closeable

/**
 * MVI dispatcher responsible for processing intentions and producing outcomes.
 *
 * @param S The type of the outcome produced by the dispatcher.
 */
interface IntentionDispatcher<S : State> : Closeable {

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
}
