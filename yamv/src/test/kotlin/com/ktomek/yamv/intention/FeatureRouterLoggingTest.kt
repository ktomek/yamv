package com.ktomek.yamv.intention

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.State
import com.ktomek.yamv.logging.Yamv
import com.ktomek.yamv.logging.YamvLogLevel
import com.ktomek.yamv.logging.YamvLogger
import com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

private data class LoggingTestState(val value: Int = 0) : State

class FeatureRouterLoggingTest {

    @AfterEach
    fun tearDown() {
        Yamv.reset()
    }

    @Test
    fun `initialization is logged at INFO level`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val logs = mutableListOf<Pair<YamvLogLevel, String>>()
        Yamv.logLevel = YamvLogLevel.DEBUG
        Yamv.logger = YamvLogger { level, _, message -> logs.add(level to message) }

        val router = FeatureRouter<LoggingTestState>(emptySet())
        router.initialize(
            scope = CoroutineScope(SupervisorJob() + testDispatcher),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(testDispatcher, testDispatcher),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(logs.any { it.first == YamvLogLevel.INFO }).isTrue()
    }

    @Test
    fun `intention dispatch is logged at VERBOSE level`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val logs = mutableListOf<Pair<YamvLogLevel, String>>()
        Yamv.logLevel = YamvLogLevel.VERBOSE
        Yamv.logger = YamvLogger { level, _, message -> logs.add(level to message) }

        val router = FeatureRouter<LoggingTestState>(emptySet())
        router.initialize(
            scope = CoroutineScope(SupervisorJob() + testDispatcher),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(testDispatcher, testDispatcher),
        )
        router.dispatchIntention("TestIntention")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(logs.any { it.first == YamvLogLevel.VERBOSE }).isTrue()
    }

    @Test
    fun `no logs emitted when log level is NONE`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val logs = mutableListOf<Pair<YamvLogLevel, String>>()
        Yamv.logLevel = YamvLogLevel.NONE
        Yamv.logger = YamvLogger { level, _, message -> logs.add(level to message) }

        val router = FeatureRouter<LoggingTestState>(emptySet())
        router.initialize(
            scope = CoroutineScope(SupervisorJob() + testDispatcher),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(testDispatcher, testDispatcher),
        )
        router.dispatchIntention("TestIntention")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(logs).isEmpty()
    }
}
