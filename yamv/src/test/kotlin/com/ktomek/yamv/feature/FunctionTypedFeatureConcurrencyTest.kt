package com.ktomek.yamv.feature

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig
import com.ktomek.yamv.state.MviRuntime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

private data class ConcurrencyTestState(val results: List<String> = emptyList()) : State

@OptIn(ExperimentalCoroutinesApi::class)
class FunctionTypedFeatureConcurrencyTest {

    @Test
    fun `concurrent intentions are processed concurrently not sequentially`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val processingOrder = mutableListOf<String>()

        val slowFeature = FunctionTypedFeature<ConcurrencyTestState, String> { intention ->
            delay(100)
            processingOrder.add("completed:$intention")
            StateOutcome { state -> state.copy(results = state.results + intention) }
        }

        val runtime = MviRuntime(
            features = setOf(slowFeature.wrap()),
            defaultState = ConcurrencyTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        // Allow feature coroutines to subscribe before dispatching
        testScheduler.advanceUntilIdle()

        runtime.dispatch("A")
        runtime.dispatch("B")
        runtime.dispatch("C")

        // Kick off the dispatch coroutines
        testScheduler.advanceUntilIdle()

        // With sequential processing (old behavior), this would take 300ms virtual time.
        // With concurrent processing (new behavior), all 3 complete by 100ms virtual time.
        testScheduler.advanceTimeBy(110)

        assertThat(processingOrder).hasSize(3)
        runtime.clear()
    }

    @Test
    fun `outcomes from concurrent intentions all reach state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val slowFeature = FunctionTypedFeature<ConcurrencyTestState, String> { intention ->
            delay(50)
            StateOutcome { state -> state.copy(results = state.results + intention) }
        }

        val runtime = MviRuntime(
            features = setOf(slowFeature.wrap()),
            defaultState = ConcurrencyTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        // Allow feature coroutines to subscribe before dispatching
        testScheduler.advanceUntilIdle()

        runtime.dispatch("X")
        runtime.dispatch("Y")
        runtime.dispatch("Z")

        testScheduler.advanceUntilIdle()

        assertThat(runtime.state.value.results).containsExactlyElementsIn(listOf("X", "Y", "Z"))
        runtime.clear()
    }

    @Test
    fun `single slow intention does not block unrelated intentions from other features`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val slowFeature = FunctionTypedFeature<ConcurrencyTestState, Int> { _ ->
            delay(1_000)
            StateOutcome { state -> state.copy(results = state.results + "slow") }
        }

        val fastFeature = FunctionTypedFeature<ConcurrencyTestState, String> { intention ->
            StateOutcome { state -> state.copy(results = state.results + intention) }
        }

        val runtime = MviRuntime(
            features = setOf(slowFeature.wrap(), fastFeature.wrap()),
            defaultState = ConcurrencyTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        // Allow feature coroutines to subscribe before dispatching
        testScheduler.advanceUntilIdle()

        runtime.dispatch(42)
        runtime.dispatch("fast1")
        runtime.dispatch("fast2")

        testScheduler.advanceTimeBy(10)
        assertThat(runtime.state.value.results).containsAtLeast("fast1", "fast2")
        assertThat(runtime.state.value.results).doesNotContain("slow")

        runtime.clear()
    }
}
