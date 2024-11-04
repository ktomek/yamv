package com.ktomek.yamv.feature

import com.ktomek.yamv.state.Store
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Extension property to get the coroutine scope for the feature.
 *
 * @receiver Feature<*>
 * @return The coroutine scope used internally by the feature.
 */
val Feature<*>.featureScope: CoroutineScope
    get() = internalFeatureScope

/**
 * Abstract class representing a feature that processes intentions and produces outcomes.
 *
 * @param OUTCOME The type of the outcome produced by the feature.
 */
abstract class Feature<OUTCOME> : IFeature by DefaultFeature() {

    /**
     * Receives an intention and produces an outcome.
     *
     * @param intention The intention to be processed.
     * @param store The store to be used in the feature function.
     * @return The outcome produced by the feature function.
     */
    abstract suspend operator fun invoke(intention: Any, store: Store): OUTCOME

    /**
     * Gets the coroutine dispatcher for the feature.
     *
     * @return The coroutine dispatcher used by the feature, or null if not specified.
     */
    open val dispatcher: CoroutineDispatcher?
        get() = null
}
