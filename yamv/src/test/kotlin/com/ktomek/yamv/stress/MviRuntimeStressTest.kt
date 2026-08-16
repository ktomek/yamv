package com.ktomek.yamv.stress

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.feature.TypedFeature
import com.ktomek.yamv.feature.wrap
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.MviRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

private data class StressState(
    val count: Int = 0,
    val slowCount: Int = 0,
    val fastCount: Int = 0,
) : State

private sealed class StressIntention {
    data object Increment : StressIntention()
    data object SlowAction : StressIntention()
    data object FastAction : StressIntention()
}

/** Mirrors production: single-threaded intentions/reducers, multi-threaded features. */
private fun realDispatcherConfig() = object : CoroutineDispatcherConfig {
    override fun provideIntentionDispatcher(intention: Any?) = Dispatchers
        .Default
        .limitedParallelism(1)

    override fun provideReducerDispatcher() = Dispatchers
        .Default
        .limitedParallelism(1)

    override fun provideFeatureDispatcher(feature: Any) = Dispatchers.Default
}

/** All multi-threaded — worst-case scenario for race conditions. */
private fun multiThreadDispatcherConfig() = object : CoroutineDispatcherConfig {
    override fun provideIntentionDispatcher(intention: Any?) = Dispatchers.Default
    override fun provideReducerDispatcher() = Dispatchers.Default
    override fun provideFeatureDispatcher(feature: Any) = Dispatchers.Default
}

private suspend fun awaitState(
    runtime: MviRuntime<StressState>,
    timeoutMs: Long = 5000,
    condition: (StressState) -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition(runtime.state.value)) {
        if (System.currentTimeMillis() > deadline) break
        delay(50)
    }
}

@Timeout(30, unit = TimeUnit.SECONDS)
class MviRuntimeStressTest {

    @RepeatedTest(10)
    fun `state remains consistent when dispatching from multiple threads`() = runBlocking {
        val feature = FunctionTypedFeature<StressState, StressIntention.Increment> {
            StateOutcome { state -> state.copy(count = state.count + 1) }
        }

        val runtime = MviRuntime(
            features = setOf(feature.wrap()),
            defaultState = StressState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        coroutineScope {
            repeat(8) {
                launch(Dispatchers.Default) {
                    repeat(1_250) {
                        runtime.dispatch(StressIntention.Increment)
                    }
                }
            }
        }

        awaitState(runtime, timeoutMs = 10_000) { it.count == 10_000 }
        assertThat(runtime.state.value.count).isEqualTo(10_000)
        runtime.clear()
    }

    @RepeatedTest(10)
    fun `state remains consistent with multi-threaded intentions and reducers`() = runBlocking {
        val feature = FunctionTypedFeature<StressState, StressIntention.Increment> {
            StateOutcome { state -> state.copy(count = state.count + 1) }
        }

        val runtime = MviRuntime(
            features = setOf(feature.wrap()),
            defaultState = StressState(),
            dispatcherConfig = multiThreadDispatcherConfig(),
        )

        delay(100)

        coroutineScope {
            repeat(8) {
                launch(Dispatchers.Default) {
                    repeat(1_250) {
                        runtime.dispatch(StressIntention.Increment)
                    }
                }
            }
        }

        awaitState(runtime, timeoutMs = 10_000) { it.count == 10_000 }
        assertThat(runtime.state.value.count).isEqualTo(10_000)
        runtime.clear()
    }

    @RepeatedTest(5)
    fun `mixed feature types produce correct total under load`() = runBlocking {
        val functionFeature = FunctionTypedFeature<StressState, StressIntention.Increment> {
            StateOutcome { state -> state.copy(count = state.count + 1) }
        }

        val typedFeature = TypedFeature<StressState, StressIntention.Increment> { intention ->
            intention.map { StateOutcome { state -> state.copy(count = state.count + 1) } }
        }

        val flowFeature = Feature.FlowFeature { intentions ->
            intentions
                .filterIsInstance<StressIntention.Increment>()
                .map { StateOutcome<StressState> { state -> state.copy(count = state.count + 1) } }
        }

        val runtime = MviRuntime(
            features = setOf(functionFeature.wrap(), typedFeature.wrap(), flowFeature),
            defaultState = StressState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        delay(100)

        coroutineScope {
            repeat(4) {
                launch(Dispatchers.Default) {
                    repeat(250) {
                        runtime.dispatch(StressIntention.Increment)
                    }
                }
            }
        }

        // 1000 intentions × 3 features = 3000 increments
        awaitState(runtime, timeoutMs = 10_000) { it.count == 3_000 }
        assertThat(runtime.state.value.count).isEqualTo(3_000)
        runtime.clear()
    }

    @RepeatedTest(5)
    fun `handles backpressure when dispatching faster than features consume`() = runBlocking {
        val slowFeature = FunctionTypedFeature<StressState, StressIntention.Increment> {
            delay(1)
            StateOutcome { state -> state.copy(count = state.count + 1) }
        }

        val runtime = MviRuntime(
            features = setOf(slowFeature.wrap()),
            defaultState = StressState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        delay(100)

        // Dispatch 500 as fast as possible — exceeds buffer of 64
        repeat(500) {
            runtime.dispatch(StressIntention.Increment)
        }

        awaitState(runtime, timeoutMs = 10_000) { it.count == 500 }
        assertThat(runtime.state.value.count).isEqualTo(500)
        runtime.clear()
    }

    @RepeatedTest(5)
    fun `slow feature does not block fast feature`() = runBlocking {
        val fastFeature = FunctionTypedFeature<StressState, StressIntention.FastAction> {
            StateOutcome { state -> state.copy(fastCount = state.fastCount + 1) }
        }

        val slowFeature = FunctionTypedFeature<StressState, StressIntention.SlowAction> {
            delay(50)
            StateOutcome { state -> state.copy(slowCount = state.slowCount + 1) }
        }

        val runtime = MviRuntime(
            features = setOf(fastFeature.wrap(), slowFeature.wrap()),
            defaultState = StressState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        delay(100)

        // Dispatch fast and slow intentions
        repeat(100) { runtime.dispatch(StressIntention.FastAction) }
        repeat(10) { runtime.dispatch(StressIntention.SlowAction) }

        // Fast features should finish well before slow ones
        delay(200)
        assertThat(runtime.state.value.fastCount).isEqualTo(100)

        // Slow features eventually complete
        awaitState(runtime, timeoutMs = 5_000) { it.slowCount == 10 }
        assertThat(runtime.state.value.slowCount).isEqualTo(10)
        runtime.clear()
    }
}
