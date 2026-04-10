package com.ktomek.yamv.feature

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

inline fun <reified S : State, reified INTENTION> FunctionTypedFeature<S, INTENTION>.wrap(): Feature<S> =
    object : TypedFeatureHolder<S> {
        override val feature: Any = this@wrap

        override fun invoke(intentions: Flow<Any>): Flow<Outcome<S>> =
            channelFlow {
                intentions
                    .filterIsInstance<INTENTION>()
                    .collect { intention ->
                        launch { send(this@wrap.invoke(intention)) }
                    }
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
