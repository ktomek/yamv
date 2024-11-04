package com.ktomek.yamv.feature

import com.ktomek.yamv.state.Store

/**
 * Creates a `TypedFeature` instance with the specified feature function.
 *
 * @param OUTCOME The type of the outcome produced by the feature.
 * @param INTENTION The type of the intention processed by the feature.
 * @param feature A suspend function that takes an intention and a store, and produces an outcome.
 * @return A `TypedFeature` instance that invokes the provided feature function.
 * Example:
 * ```
 * val feature = typedFeature<CounterOutcome, CounterIntention> { intention, store ->
 *     ChangeCounterOutcome(intention.value)
 * }
 *  ```
 */
inline fun <reified OUTCOME, reified INTENTION> typedFeature(
    crossinline feature: suspend (INTENTION, store: Store) -> OUTCOME
): TypedFeature<OUTCOME, INTENTION> = object : TypedFeature<OUTCOME, INTENTION>() {
    /**
     * Invokes the feature function with the given intention and store.
     *
     * @param intention The intention to be processed.
     * @param store The store to be used in the feature function.
     * @return The outcome produced by the feature function.
     */
    override suspend fun invoke(intention: INTENTION, store: Store): OUTCOME =
        feature(intention, store)
}
