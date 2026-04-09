package com.ktomek.yamv.feature

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig
import com.ktomek.yamv.state.MviRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import app.cash.turbine.test

private data class ScopedState(val ticks: Int = 0) : State
private data object StartTicking

class ScopedFeatureBuilderTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `typedFeatureWithScope can launch background coroutines`() = runTest(dispatcher) {
        val autoTickFeature = typedFeatureWithScope<ScopedState, StartTicking> { intentions ->
            intentions.collect {
                launch {
                    repeat(3) {
                        delay(100)
                        send(StateOutcome { state -> state.copy(ticks = state.ticks + 1) })
                    }
                }
            }
        }

        val runtime = MviRuntime(
            features = setOf(autoTickFeature),
            defaultState = ScopedState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()

        runtime.state.test {
            assertThat(awaitItem().ticks).isEqualTo(0)
            runtime.dispatch(StartTicking)
            testScheduler.advanceUntilIdle()
            testScheduler.advanceTimeBy(350)
            assertThat(awaitItem().ticks).isEqualTo(1)
            assertThat(awaitItem().ticks).isEqualTo(2)
            assertThat(awaitItem().ticks).isEqualTo(3)
        }

        runtime.clear()
    }

    @Test
    fun `background coroutines are cancelled when runtime is cleared`() = runTest(dispatcher) {
        val tickCount = mutableListOf<Int>()
        val autoTickFeature = typedFeatureWithScope<ScopedState, StartTicking> { intentions ->
            intentions.collect {
                launch {
                    var count = 0
                    while (isActive) {
                        delay(50)
                        tickCount.add(++count)
                        send(StateOutcome { state -> state.copy(ticks = state.ticks + 1) })
                    }
                }
            }
        }

        val runtime = MviRuntime(
            features = setOf(autoTickFeature),
            defaultState = ScopedState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        runtime.dispatch(StartTicking)
        testScheduler.advanceTimeBy(160)
        val countAtClear = tickCount.size
        assertThat(countAtClear).isAtLeast(2)

        runtime.clear()
        testScheduler.advanceTimeBy(500)

        assertThat(tickCount.size).isEqualTo(countAtClear)
    }

    @Test
    fun `typedFeatureWithScope sends outcomes correctly`() = runTest(dispatcher) {
        val feature = typedFeatureWithScope<ScopedState, StartTicking> { intentions ->
            intentions.collect {
                send(StateOutcome { state -> state.copy(ticks = 99) })
            }
        }

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = ScopedState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        runtime.dispatch(StartTicking)
        testScheduler.advanceUntilIdle()

        assertThat(runtime.state.value.ticks).isEqualTo(99)
        runtime.clear()
    }
}
