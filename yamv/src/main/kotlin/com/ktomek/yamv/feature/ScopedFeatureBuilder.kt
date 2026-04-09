package com.ktomek.yamv.feature

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance

/**
 * Creates a [Feature] with explicit access to a [ProducerScope] — a [kotlinx.coroutines.CoroutineScope]
 * that is tied to the feature's lifetime and can be used to launch background coroutines.
 *
 * The scope is the same coroutine scope used by [channelFlow]. Any [kotlinx.coroutines.launch]
 * calls inside [block] become child coroutines — they are cancelled automatically when
 * [com.ktomek.yamv.state.MviRuntime.clear] is called.
 *
 * Example — a timer that emits on every intention:
 * ```kotlin
 * val autoIncrementFeature = typedFeatureWithScope<CounterState, StartAutoIncrement> { intentions ->
 *     var timerJob: Job? = null
 *     intentions.collect {
 *         timerJob?.cancel()
 *         timerJob = launch {
 *             while (isActive) {
 *                 delay(1_000)
 *                 send(StateOutcome { state -> state.copy(count = state.count + 1) })
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param block A suspend function that receives a [ProducerScope] as receiver and a [Flow] of
 *   typed intentions. Use [ProducerScope.send] to emit outcomes and [kotlinx.coroutines.launch]
 *   to start background work.
 */
inline fun <reified S : State, reified I : Any> typedFeatureWithScope(
    crossinline block: suspend ProducerScope<Outcome<S>>.(intentions: Flow<I>) -> Unit,
): Feature<S> = object : Feature.FlowFeature<S> {
    override fun invoke(intentions: Flow<Any>): Flow<Outcome<S>> = channelFlow {
        block(intentions.filterIsInstance<I>())
    }
}
