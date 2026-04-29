package com.ktomek.yamv.feature

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow

/**
 * Abstract class representing a typed feature that processes intentions and produces outcomes.
 *
 * @param S The type of the outcome produced by the feature.
 * @param INTENTION The type of the intention processed by the feature.
 */
fun interface TypedFeature<S : State, INTENTION> {
    /**
     * Receives an intention and produces an outcome.
     *
     * @param intention The intention to be processed.
     * @param store The store to be used in the feature function.
     * @return The outcome produced by the feature function.
     */
    operator fun invoke(intention: Flow<INTENTION>): Flow<Outcome<S>>
}

interface TypedFeatureHolder<S : State> : Feature.FlowFeature<S> {
    val feature: Any
}

interface TypedUnitFeatureHolder<S : State> : Feature.FlowFeature<S> {
    val feature: Any
}

/**
 * Typed counterpart of [Feature.FlowUnitFeature]: receives a strongly-typed [Flow] of intentions
 * and produces a [Flow] of [Unit] (fire-and-forget side effects, no state contribution).
 *
 * Use [wrap] to turn a [TypedUnitFeature] into a [Feature] that the runtime can dispatch.
 *
 * @param S The state type the owning store works with.
 * @param INTENTION The intention type this feature reacts to.
 */
fun interface TypedUnitFeature<S : State, INTENTION> {
    operator fun invoke(intention: Flow<INTENTION>): Flow<Unit>
}
