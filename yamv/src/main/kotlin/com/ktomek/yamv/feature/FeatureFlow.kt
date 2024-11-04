package com.ktomek.yamv.feature

import com.ktomek.yamv.state.Store
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Extension property to get the coroutine scope for the feature flow.
 *
 * @receiver FeatureFlow<*>
 * @return The coroutine scope used internally by the feature flow.
 */
val FeatureFlow<*>.featureScope: CoroutineScope
    get() = internalFeatureScope

/**
 * Abstract class representing a feature flow that processes a stream of intentions and produces outcomes.
 *
 * @param OUTCOME The type of the outcome produced by the feature flow.
 */
abstract class FeatureFlow<OUTCOME> : IFeature by DefaultFeature() {

    /**
     * Observes a stream of intentions and produces a stream of outcomes.
     *
     * @param intentions The stream of intentions to be processed.
     * @param store The store to be used in the feature function.
     * @return The stream of outcomes produced by the feature function.
     */
    abstract suspend operator fun invoke(intentions: Flow<Any>, store: Store): Flow<OUTCOME>

    /**
     * Gets the coroutine dispatcher for the feature flow.
     *
     * @return The coroutine dispatcher used by the feature flow, or null if not specified.
     */
    open val dispatcher: CoroutineDispatcher?
        get() = null
}
