package com.ktomek.yamv.stress

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.MviRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

private data class FlowFeatureTestState(
    val count: Int = 0,
    val visited: List<Int> = emptyList(),
) : State

/** Mirrors production: single-threaded intentions/reducers, multi-threaded features. */
private fun realDispatcherConfig() = object : CoroutineDispatcherConfig {
    override fun provideIntentionDispatcher(intention: Any?) =
        Dispatchers.Default.limitedParallelism(1)

    override fun provideReducerDispatcher() =
        Dispatchers.Default.limitedParallelism(1)

    override fun provideFeatureDispatcher(feature: Any) = Dispatchers.Default
}

private suspend fun <S : State> awaitState(
    runtime: MviRuntime<S>,
    timeoutMs: Long = 5_000,
    condition: (S) -> Boolean,
) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition(runtime.state.value)) {
        if (System.currentTimeMillis() > deadline) break
        delay(20)
    }
}

@Timeout(15, unit = TimeUnit.SECONDS)
class MviRuntimeFlowFeatureIntegrationTest {

    @Test
    fun `FlowFeature emitting own StateOutcomes eagerly reduces each into state`() = runBlocking {
        // Arrange — a FlowFeature that ignores intentions and emits its own stream of
        // StateOutcomes. Delay the first emission slightly so the reducer collector
        // has time to subscribe before the burst starts.
        val selfEmittingFeature = Feature.FlowFeature<FlowFeatureTestState> { _: Flow<Any> ->
            flowOf<Outcome<FlowFeatureTestState>>(
                StateOutcome { state -> state.copy(count = state.count + 1, visited = state.visited + 1) },
                StateOutcome { state -> state.copy(count = state.count + 1, visited = state.visited + 2) },
                StateOutcome { state -> state.copy(count = state.count + 1, visited = state.visited + 3) },
            )
        }

        val runtime = MviRuntime(
            features = setOf(selfEmittingFeature),
            defaultState = FlowFeatureTestState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        // Act — wait for all three outcomes to land in state
        awaitState(runtime) { it.visited.size == 3 }

        // Assert — all three StateOutcomes applied in order
        val state = runtime.state.value
        assertThat(state.count).isEqualTo(3)
        assertThat(state.visited).containsExactly(1, 2, 3).inOrder()
        runtime.clear()
    }

    @Test
    fun `FlowFeature emitting many outcomes quickly all reach state`() = runBlocking {
        // Arrange — burst-emit 100 StateOutcomes from a single FlowFeature.
        val burstOutcomes: Array<Outcome<FlowFeatureTestState>> = Array(100) {
            StateOutcome<FlowFeatureTestState> { state -> state.copy(count = state.count + 1) }
        }
        val burstFeature = Feature.FlowFeature<FlowFeatureTestState> { _ ->
            flowOf(*burstOutcomes)
        }

        val runtime = MviRuntime(
            features = setOf(burstFeature),
            defaultState = FlowFeatureTestState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        // Act
        awaitState(runtime) { it.count == 100 }

        // Assert
        assertThat(runtime.state.value.count).isEqualTo(100)
        runtime.clear()
    }

    @Test
    fun `multiple FlowFeatures emitting concurrently both contribute to final state`() = runBlocking {
        // Arrange — two independent FlowFeatures each emit their own outcomes.
        val featureA = Feature.FlowFeature<FlowFeatureTestState> { _ ->
            flowOf<Outcome<FlowFeatureTestState>>(
                StateOutcome { state -> state.copy(count = state.count + 10) },
                StateOutcome { state -> state.copy(count = state.count + 10) },
            )
        }
        val featureB = Feature.FlowFeature<FlowFeatureTestState> { _ ->
            flowOf<Outcome<FlowFeatureTestState>>(
                StateOutcome { state -> state.copy(count = state.count + 1) },
                StateOutcome { state -> state.copy(count = state.count + 1) },
                StateOutcome { state -> state.copy(count = state.count + 1) },
            )
        }

        val runtime = MviRuntime(
            features = setOf(featureA, featureB),
            defaultState = FlowFeatureTestState(),
            dispatcherConfig = realDispatcherConfig(),
        )

        // Act
        awaitState(runtime) { it.count == 23 }

        // Assert — 10 + 10 + 1 + 1 + 1 = 23, regardless of interleaving
        assertThat(runtime.state.value.count).isEqualTo(23)
        runtime.clear()
    }
}
