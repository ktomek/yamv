package com.ktomek.yamv.state

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.logging.Yamv
import com.ktomek.yamv.logging.YamvLogLevel
import com.ktomek.yamv.logging.YamvLogger
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

private data class LogTestState(val x: Int = 0) : State

class MviRuntimeLoggingTest {

    private val dispatcher = StandardTestDispatcher()

    @AfterEach
    fun tearDown() {
        Yamv.reset()
    }

    @Test
    fun `runtime creation is logged at DEBUG level`() = runTest(dispatcher) {
        val logs = mutableListOf<Pair<YamvLogLevel, String>>()
        Yamv.logLevel = YamvLogLevel.DEBUG
        Yamv.logger = YamvLogger { level, _, message -> logs.add(level to message) }

        MviRuntime(
            features = emptySet(),
            defaultState = LogTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )
        testScheduler.advanceUntilIdle()

        assertThat(logs.any { it.first == YamvLogLevel.DEBUG }).isTrue()
    }

    @Test
    fun `clear is logged at INFO level`() = runTest(dispatcher) {
        val logs = mutableListOf<Pair<YamvLogLevel, String>>()
        Yamv.logLevel = YamvLogLevel.INFO
        Yamv.logger = YamvLogger { level, _, message -> logs.add(level to message) }

        val runtime = MviRuntime(
            features = emptySet(),
            defaultState = LogTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )
        testScheduler.advanceUntilIdle()

        val logsBefore = logs.size
        runtime.clear()

        assertThat(logs.size).isGreaterThan(logsBefore)
        assertThat(logs.last().first).isEqualTo(YamvLogLevel.INFO)
    }

    @Test
    fun `state update is logged at VERBOSE level`() = runTest(dispatcher) {
        val logs = mutableListOf<Pair<YamvLogLevel, String>>()
        Yamv.logLevel = YamvLogLevel.VERBOSE
        Yamv.logger = YamvLogger { level, _, message -> logs.add(level to message) }

        val feature = com.ktomek.yamv.feature.functionTypedFeature<LogTestState, String> { _ ->
            StateOutcome { state -> state.copy(x = state.x + 1) }
        }

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = LogTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )
        testScheduler.advanceUntilIdle()

        runtime.dispatch("trigger")
        testScheduler.advanceUntilIdle()

        assertThat(logs.any { it.first == YamvLogLevel.VERBOSE && it.second.contains("State updated") }).isTrue()
        runtime.clear()
    }

    @Test
    fun `no logs when log level is NONE`() = runTest(dispatcher) {
        val logs = mutableListOf<Pair<YamvLogLevel, String>>()
        Yamv.logLevel = YamvLogLevel.NONE
        Yamv.logger = YamvLogger { level, _, message -> logs.add(level to message) }

        val runtime = MviRuntime(
            features = emptySet(),
            defaultState = LogTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )
        runtime.dispatch("intention")
        testScheduler.advanceUntilIdle()
        runtime.clear()

        assertThat(logs).isEmpty()
    }
}
