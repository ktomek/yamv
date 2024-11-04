package com.ktomek.yamv.intention

import kotlinx.coroutines.flow.SharedFlow
import java.io.Closeable

/**
 * MVI dispatcher responsible for processing intentions and producing results.
 *
 * @param Result The type of the result produced by the dispatcher.
 */
interface IntentionDispatcher<Result> : Closeable {

    /**
     * Listens for intentions from upstream and processes them.
     *
     * @return A [SharedFlow] emitting the results produced by the dispatcher.
     */
    fun observeResults(): SharedFlow<Result>

    /**
     * Processes a single intention.
     *
     * @param intention The intention to be processed.
     */
    suspend fun dispatchIntention(intention: Any)
}
