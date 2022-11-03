package com.ktomek.yamv.intention

import kotlinx.coroutines.flow.SharedFlow
import java.io.Closeable

/**
 * MVI dispatcher responsible for process intention and [Result] production.
 */
interface IntentionDispatcher<Result> : Closeable {

    /**
     * Listening for intention from upstream and processing them.
     */
    fun observeResults(): SharedFlow<Result>

    /**
     * Process single intention
     */
    suspend fun dispatchIntention(intention: Any)
}
