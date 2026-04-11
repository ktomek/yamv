package com.ktomek.yamv.stress

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.IntentionOutcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.feature.wrap
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.ErrorSource
import com.ktomek.yamv.state.MviExceptionHandler
import com.ktomek.yamv.state.MviRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private data class LifecycleState(
    val count: Int = 0,
    val history: List<Int> = emptyList(),
) : State

private sealed class LifecycleIntention {
    data object Increment : LifecycleIntention()
    data object Crash : LifecycleIntention()
    data class ChainTo(val next: Any) : LifecycleIntention()
}

private fun realDispatcherConfig() = object : CoroutineDispatcherConfig {
    override fun provideIntentionDispatcher(intention: Any?) =
        Dispatchers.Default.limitedParallelism(1)

    override fun provideReducerDispatcher() =
        Dispatchers.Default.limitedParallelism(1)

    override fun provideFeatureDispatcher(feature: Any) = Dispatchers.Default
}

private suspend fun <S : State> awaitState(
    runtime: MviRuntime<S>,
    timeoutMs: Long = 5000,
    condition: (S) -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition(runtime.state.value)) {
        if (System.currentTimeMillis() > deadline) break
        delay(50)
    }
}

@Timeout(30, unit = TimeUnit.SECONDS)
class MviRuntimeLifecycleStressTest {

    @RepeatedTest(5)
    fun `clear during active feature processing does not crash`() = runBlocking {
        val feature = FunctionTypedFeature<LifecycleState, LifecycleIntention.Increment> {
            delay(10)
            StateOutcome { state -> state.copy(count = state.count + 1) }
        }

        val runtime = MviRuntime(
            features = setOf(feature.wrap()),
            defaultState = LifecycleState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        delay(100)

        // Dispatch many slow intentions
        repeat(100) { runtime.dispatch(LifecycleIntention.Increment) }

        // Clear while features are still processing
        delay(50)
        runtime.clear()

        // State should be frozen after clear — no more updates
        val stateAfterClear = runtime.state.value.count
        delay(200)
        assertThat(runtime.state.value.count).isEqualTo(stateAfterClear)
    }

    @RepeatedTest(5)
    fun `concurrent dispatch and clear does not crash`() = runBlocking {
        val feature = FunctionTypedFeature<LifecycleState, LifecycleIntention.Increment> {
            StateOutcome { state -> state.copy(count = state.count + 1) }
        }

        val runtime = MviRuntime(
            features = setOf(feature.wrap()),
            defaultState = LifecycleState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        delay(100)

        // Race: dispatch from multiple threads while clearing
        coroutineScope {
            repeat(4) {
                launch(Dispatchers.Default) {
                    repeat(500) {
                        try {
                            runtime.dispatch(LifecycleIntention.Increment)
                        } catch (_: Exception) {
                            // Expected: scope may be cancelled
                        }
                    }
                }
            }
            launch(Dispatchers.Default) {
                delay(10)
                runtime.clear()
            }
        }

        // Should not crash — just verify we get here
        assertThat(runtime.state.value.count).isAtLeast(0)
    }

    @Test
    fun `reducer exception triggers exception handler with REDUCER source`() = runBlocking {
        val handlerCalled = AtomicBoolean(false)
        val capturedSource = CopyOnWriteArrayList<ErrorSource>()

        val feature = FunctionTypedFeature<LifecycleState, LifecycleIntention.Crash> {
            StateOutcome { _ -> throw IllegalStateException("reducer boom") }
        }

        val runtime = MviRuntime(
            features = setOf(feature.wrap()),
            defaultState = LifecycleState(),
            dispatcherConfig = realDispatcherConfig(),
            exceptionHandler = MviExceptionHandler { context, _ ->
                handlerCalled.set(true)
                capturedSource.add(context.source)
            },
        )

        delay(100)
        runtime.dispatch(LifecycleIntention.Crash)
        delay(500)

        assertThat(handlerCalled.get()).isTrue()
        assertThat(capturedSource).contains(ErrorSource.REDUCER)
        runtime.clear()
    }

    @Test
    fun `feature exception triggers exception handler with FEATURE source`() = runBlocking {
        val handlerCalled = AtomicBoolean(false)
        val capturedSource = CopyOnWriteArrayList<ErrorSource>()

        val feature = FunctionTypedFeature<LifecycleState, LifecycleIntention.Crash> {
            throw IllegalStateException("feature boom")
        }

        val runtime = MviRuntime(
            features = setOf(feature.wrap()),
            defaultState = LifecycleState(),
            dispatcherConfig = realDispatcherConfig(),
            exceptionHandler = MviExceptionHandler { context, _ ->
                handlerCalled.set(true)
                capturedSource.add(context.source)
            },
        )

        delay(100)
        runtime.dispatch(LifecycleIntention.Crash)
        delay(500)

        assertThat(handlerCalled.get()).isTrue()
        assertThat(capturedSource).contains(ErrorSource.FEATURE)
        runtime.clear()
    }

    @Test
    fun `default exception handler cancels scope on reducer failure`() = runBlocking {
        val feature = FunctionTypedFeature<LifecycleState, LifecycleIntention.Crash> {
            StateOutcome { _ -> throw IllegalStateException("reducer crash") }
        }
        val incrementFeature = FunctionTypedFeature<LifecycleState, LifecycleIntention.Increment> {
            StateOutcome { state -> state.copy(count = state.count + 1) }
        }

        val runtime = MviRuntime(
            features = setOf(feature.wrap(), incrementFeature.wrap()),
            defaultState = LifecycleState(),
            dispatcherConfig = realDispatcherConfig(),
            // Default handler rethrows — scope should be cancelled
        )

        delay(100)
        runtime.dispatch(LifecycleIntention.Crash)
        delay(500)

        // After scope cancellation, further dispatches should have no effect
        val stateAfterCrash = runtime.state.value.count
        try {
            runtime.dispatch(LifecycleIntention.Increment)
        } catch (_: Exception) {
            // Expected
        }
        delay(200)
        assertThat(runtime.state.value.count).isEqualTo(stateAfterCrash)
        runtime.clear()
    }

    @Test
    fun `custom exception handler allows degraded operation`() = runBlocking {
        val errorCount = AtomicInteger(0)

        val crashFeature = FunctionTypedFeature<LifecycleState, LifecycleIntention.Crash> {
            StateOutcome { _ -> throw IllegalStateException("boom") }
        }
        val incrementFeature = FunctionTypedFeature<LifecycleState, LifecycleIntention.Increment> {
            StateOutcome { state -> state.copy(count = state.count + 1) }
        }

        val runtime = MviRuntime(
            features = setOf(crashFeature.wrap(), incrementFeature.wrap()),
            defaultState = LifecycleState(),
            dispatcherConfig = realDispatcherConfig(),
            exceptionHandler = MviExceptionHandler { _, _ ->
                // Don't rethrow — degraded mode
                errorCount.incrementAndGet()
            },
        )

        delay(100)

        // Crash reducer but don't rethrow — runtime should continue
        runtime.dispatch(LifecycleIntention.Crash)
        delay(200)

        // Increment should still work because handler didn't rethrow
        runtime.dispatch(LifecycleIntention.Increment)
        awaitState(runtime, timeoutMs = 2_000) { it.count >= 1 }

        assertThat(errorCount.get()).isAtLeast(1)
        assertThat(runtime.state.value.count).isAtLeast(1)
        runtime.clear()
    }

    @Test
    fun `state transitions are recorded in order`() = runBlocking {
        val feature = FunctionTypedFeature<LifecycleState, LifecycleIntention.Increment> {
            StateOutcome { state ->
                state.copy(
                    count = state.count + 1,
                    history = state.history + (state.count + 1),
                )
            }
        }

        val runtime = MviRuntime(
            features = setOf(feature.wrap()),
            defaultState = LifecycleState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        delay(100)

        repeat(20) { runtime.dispatch(LifecycleIntention.Increment) }

        awaitState(runtime, timeoutMs = 5_000) { it.count == 20 }

        // History should be strictly monotonically increasing
        val history = runtime.state.value.history
        assertThat(history).hasSize(20)
        assertThat(history).isEqualTo((1..20).toList())
        runtime.clear()
    }

    @RepeatedTest(5)
    fun `no state emissions occur after clear`() = runBlocking {
        val emissionCount = AtomicInteger(0)

        val feature = FunctionTypedFeature<LifecycleState, LifecycleIntention.Increment> {
            delay(5)
            StateOutcome { state -> state.copy(count = state.count + 1) }
        }

        val runtime = MviRuntime(
            features = setOf(feature.wrap()),
            defaultState = LifecycleState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        delay(100)

        // Start observing state changes
        val observerJob = launch(Dispatchers.Default) {
            runtime.state.collect { emissionCount.incrementAndGet() }
        }

        repeat(50) { runtime.dispatch(LifecycleIntention.Increment) }
        delay(50)
        runtime.clear()

        val countAtClear = emissionCount.get()
        delay(500)

        // No new emissions after clear
        assertThat(emissionCount.get()).isEqualTo(countAtClear)
        observerJob.cancel()
    }

    @Test
    fun `intention re-dispatch chain completes without stack overflow`() = runBlocking {
        val chainDepth = AtomicInteger(0)
        val maxDepth = 50

        val chainFeature = FunctionTypedFeature<LifecycleState, LifecycleIntention.ChainTo> { intention ->
            val depth = chainDepth.incrementAndGet()
            if (depth < maxDepth) {
                object : IntentionOutcome<LifecycleState> {
                    override val intention: Any = LifecycleIntention.ChainTo(Unit)
                }
            } else {
                StateOutcome { state -> state.copy(count = depth) }
            }
        }

        val runtime = MviRuntime(
            features = setOf(chainFeature.wrap()),
            defaultState = LifecycleState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        delay(100)
        runtime.dispatch(LifecycleIntention.ChainTo(Unit))

        awaitState(runtime, timeoutMs = 5_000) { it.count == maxDepth }
        assertThat(runtime.state.value.count).isEqualTo(maxDepth)
        runtime.clear()
    }
}
