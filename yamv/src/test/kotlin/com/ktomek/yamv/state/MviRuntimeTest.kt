package com.ktomek.yamv.state

import app.cash.turbine.test
import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.IntentionOutcome
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.Feature.FlowFeature
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private interface TestState : State {
    val count: Int
}

private data class TestStateImpl(override val count: Int = 0) : TestState

private fun testDispatcherConfig(dispatcher: CoroutineDispatcher) = object : CoroutineDispatcherConfig {
    override fun provideIntentionDispatcher(intention: Any?) = dispatcher
    override fun provideReducerDispatcher() = dispatcher
    override fun provideFeatureDispatcher(feature: Any) = dispatcher
}

class MviRuntimeTest {

    @Test
    fun `GIVEN MviRuntime WHEN created THEN default state is accessible`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val defaultState = TestStateImpl(count = 0)
        val runtime = MviRuntime(
            features = emptySet(),
            defaultState = defaultState,
            dispatcherConfig = testDispatcherConfig(testDispatcher)
        )

        // Act & Assert
        assertEquals(defaultState, runtime.state.value)
    }

    @Test
    fun `GIVEN MviRuntime with a StateOutcome feature WHEN intention dispatched THEN state is updated`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val defaultState = TestStateImpl(count = 0)
        val incrementReducer: StateOutcome<TestState> = StateOutcome { prev -> TestStateImpl(prev.count + 1) }

        val feature = object : FlowFeature<TestState> {
            override fun invoke(intentions: Flow<Any>): Flow<Outcome<TestState>> =
                intentions.map { incrementReducer }
        }

        val runtime = MviRuntime(
            features = setOf<Feature<TestState>>(feature),
            defaultState = defaultState,
            dispatcherConfig = testDispatcherConfig(testDispatcher)
        )

        // Act
        runtime.dispatch("any_intention")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(1, runtime.state.value.count)
    }

    @Test
    fun `GIVEN MviRuntime with an EffectOutcome WHEN intention dispatched THEN effect is emitted`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val defaultState = TestStateImpl(count = 0)
        val testEffect = object : EffectOutcome<TestState> {}

        val feature = object : FlowFeature<TestState> {
            override fun invoke(intentions: Flow<Any>): Flow<Outcome<TestState>> =
                intentions.map { testEffect }
        }

        val runtime = MviRuntime(
            features = setOf<Feature<TestState>>(feature),
            defaultState = defaultState,
            dispatcherConfig = testDispatcherConfig(testDispatcher)
        )

        // Act & Assert
        runtime.effects.test {
            runtime.dispatch("intention")
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(testEffect, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN MviRuntime with an IntentionOutcome WHEN intention dispatched THEN intention is re-dispatched`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val defaultState = TestStateImpl(count = 0)
        var dispatchCount = 0

        val feature = object : FlowFeature<TestState> {
            override fun invoke(intentions: Flow<Any>): Flow<Outcome<TestState>> =
                intentions.map { intention ->
                    dispatchCount++
                    // Only re-dispatch once to avoid infinite loop
                    if (dispatchCount == 1) {
                        object : IntentionOutcome<TestState> {
                            override val intention: Any = "re-dispatched"
                        }
                    } else {
                        StateOutcome { prev -> prev }
                    }
                }
        }

        val runtime = MviRuntime(
            features = setOf<Feature<TestState>>(feature),
            defaultState = defaultState,
            dispatcherConfig = testDispatcherConfig(testDispatcher)
        )

        // Act
        runtime.dispatch("first_intention")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - verify re-dispatch occurred (2 total dispatches)
        assertEquals(2, dispatchCount)

        runtime.clear()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `GIVEN MviRuntime WHEN clear is called THEN no more emissions occur`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val defaultState = TestStateImpl(count = 0)
        var emissionCount = 0

        val feature = object : FlowFeature<TestState> {
            override fun invoke(intentions: Flow<Any>): Flow<Outcome<TestState>> =
                intentions.map {
                    emissionCount++
                    StateOutcome { prev -> TestStateImpl(prev.count + 1) }
                }
        }

        val runtime = MviRuntime(
            features = setOf<Feature<TestState>>(feature),
            defaultState = defaultState,
            dispatcherConfig = testDispatcherConfig(testDispatcher)
        )

        // Act
        runtime.dispatch("intention")
        testDispatcher.scheduler.advanceUntilIdle()
        val emissionCountBeforeClear = emissionCount

        runtime.clear()
        testDispatcher.scheduler.advanceUntilIdle()

        // Try to dispatch after clear - should not increase emission count
        runtime.dispatch("another_intention")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals(emissionCountBeforeClear, emissionCount)
    }

    @Test
    fun `GIVEN MviRuntime mid-emission WHEN cleared THEN exceptionHandler is never invoked for cancellation`() =
        runTest {
            // Arrange — a feature whose flow is suspended (collects but never emits) so
            // that clear() must cancel it mid-collection.
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val defaultState = TestStateImpl(count = 0)
            val recordedThrowables = mutableListOf<Throwable>()
            val recordedSources = mutableListOf<ErrorSource>()

            val handler = MviExceptionHandler { context, e ->
                recordedThrowables.add(e)
                recordedSources.add(context.source)
                throw e
            }

            val suspendingFeature = object : FlowFeature<TestState> {
                override fun invoke(intentions: Flow<Any>): Flow<Outcome<TestState>> = flow {
                    intentions.collect { /* suspend forever */ }
                }
            }

            val runtime = MviRuntime(
                features = setOf<Feature<TestState>>(suspendingFeature),
                defaultState = defaultState,
                dispatcherConfig = testDispatcherConfig(testDispatcher),
                exceptionHandler = handler,
            )

            // Act — get into a steady state, then dispatch + cancel mid-flight.
            testDispatcher.scheduler.advanceUntilIdle()
            runtime.dispatch("any")
            runtime.clear()
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert — handler must not see anything; cancellation is not an error.
            assertTrue(
                recordedThrowables.isEmpty(),
                "exceptionHandler invoked during clear() with: $recordedThrowables (sources=$recordedSources)",
            )
        }
}
