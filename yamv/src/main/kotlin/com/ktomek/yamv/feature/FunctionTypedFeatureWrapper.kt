package com.ktomek.yamv.feature

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.state.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

inline fun <reified S : State, reified INTENTION> FunctionTypedFeature<S, INTENTION>.wrap(): Feature<S> =
    Feature.FlowFeature<S> { intentions ->
        channelFlow {
            intentions
                .filterIsInstance<INTENTION>()
                .map(this@wrap::invoke)
                .collect(::send)
        }
    }

/**
 * Creates a `TypedFeature` instance with the specified feature function.
 *
 * @param S The type of the outcome produced by the feature.
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
inline fun <reified S : State, reified INTENTION> functionTypedFeature(
    crossinline feature: suspend (INTENTION) -> Outcome<S>
): Feature<S> = FunctionTypedFeature<S, INTENTION> { feature(it) }.wrap()

