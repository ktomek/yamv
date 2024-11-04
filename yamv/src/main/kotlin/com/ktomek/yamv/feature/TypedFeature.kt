package com.ktomek.yamv.feature

import com.ktomek.yamv.state.Store
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Abstract class representing a typed feature that processes intentions and produces outcomes.
 *
 * @param OUTCOME The type of the outcome produced by the feature.
 * @param INTENTION The type of the intention processed by the feature.
 */
abstract class TypedFeature<OUTCOME, INTENTION> : IFeature by DefaultFeature() {
    /**
     * Receives an intention and produces an outcome.
     *
     * @param intention The intention to be processed.
     * @param store The store to be used in the feature function.
     * @return The outcome produced by the feature function.
     */
    abstract suspend operator fun invoke(intention: INTENTION, store: Store): OUTCOME

    /**
     * Gets the coroutine scope for the feature.
     *
     * @return The coroutine scope used internally by the feature.
     */
    val featureScope: CoroutineScope
        get() = internalFeatureScope

    /**
     * Gets the coroutine dispatcher for the feature.
     *
     * @return The coroutine dispatcher used by the feature, or null if not specified.
     */
    open val dispatcher: CoroutineDispatcher?
        get() = null
}
