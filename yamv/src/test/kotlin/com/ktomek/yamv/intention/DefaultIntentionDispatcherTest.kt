package com.ktomek.yamv.intention

import app.cash.turbine.test
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.Feature.FlowFeature
import com.ktomek.yamv.feature.Feature.FlowUnitFeature
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail


// Stub State type for testing
private interface TestState : State

// Alias for outcome type
private typealias TestOutcome = Outcome<TestState>

class DefaultIntentionDispatcherTest {

    @Test
    fun `GIVEN a dispatcher with no features WHEN dispatching intentions THEN no outcomes are emitted`() =
        runTest {
            // Arrange — create a dispatcher tied to this test scheduler
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val dispatcher = DefaultIntentionDispatcher<TestState>(
                features = emptySet()
            )

            dispatcher.initialize(this, testDispatcher)

            // When / Then
            dispatcher.observeOutcomes().test {
                expectNoEvents()
            }
            dispatcher.dispatchIntention("any")
            testDispatcher.scheduler.advanceUntilIdle()
            dispatcher.observeOutcomes().test {
                expectNoEvents()
            }
        }

    @Test
    fun `GIVEN a dispatcher with one feature WHEN dispatching an intention THEN the feature processes it and emits an outcome`() =
        runTest {
            // Arrange
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val outcome = object : TestOutcome {}
            val feature = mockk<FlowFeature<TestState>> {
                every { this@mockk.invoke(any()) } answers {
                    firstArg<Flow<Any>>().map { outcome }
                }
            }
            val dispatcher = DefaultIntentionDispatcher(
                features = setOf<Feature<TestState>>(feature)
            )

            dispatcher.initialize(this, testDispatcher)
            testDispatcher.scheduler.advanceUntilIdle()

            // When & Then
            dispatcher.observeOutcomes().test {
                dispatcher.dispatchIntention("first")
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(outcome, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { feature.invoke(any()) }
            // Cancel feature coroutines launched by dispatcher to avoid leaked coroutines after runTest
            dispatcher.shutdown()
        }

    @Test
    fun `GIVEN multiple calls to dispatchIntention WHEN starting is lazy THEN features initialize only once`() =
        runTest {
            // Arrange
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val outcome = object : TestOutcome {}
            val feature = mockk<FlowFeature<TestState>> {
                every { this@mockk.invoke(any()) } answers {
                    firstArg<Flow<Any>>().map { outcome }
                }
            }
            val dispatcher = DefaultIntentionDispatcher(
                features = setOf<Feature<TestState>>(feature)
            )

            dispatcher.initialize(this, testDispatcher)

            // When
            dispatcher.dispatchIntention("first")
            dispatcher.dispatchIntention("second")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            verify(exactly = 1) { feature.invoke(any()) }
            dispatcher.shutdown()
        }

    @Test
    fun `GIVEN observeOutcomes called before dispatch WHEN init is triggered THEN outcomes are still emitted`() =
        runTest {
            // Arrange
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val outcome = object : TestOutcome {}
            val feature = mockk<FlowFeature<TestState>> {
                every { this@mockk.invoke(any()) } answers {
                    firstArg<Flow<Any>>().map { outcome }
                }
            }
            val dispatcher = DefaultIntentionDispatcher(
                features = setOf<Feature<TestState>>(feature)
            )

            dispatcher.initialize(this, testDispatcher)

            // When & Then
            dispatcher.observeOutcomes().test {
                dispatcher.dispatchIntention("something")
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(outcome, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { feature.invoke(any()) }
            dispatcher.shutdown()
        }

    @Test
    fun `GIVEN dispatchIntention called before initialize WHEN dispatching THEN throws`() =
        runTest {
            // Arrange
            val dispatcher = DefaultIntentionDispatcher<TestState>(features = emptySet())

            // Act / Assert
            try {
                dispatcher.dispatchIntention("x")
                fail("Expected IllegalStateException when dispatching before initialize")
            } catch (e: IllegalStateException) {
                // expected
            }
        }

    @Test
    fun `GIVEN observeOutcomes called before initialize WHEN observing THEN throws`() =
        runTest {
            // Arrange
            val dispatcher = DefaultIntentionDispatcher<TestState>(features = emptySet())

            // Act / Assert
            try {
                dispatcher.observeOutcomes()
                fail("Expected IllegalStateException when observing before initialize")
            } catch (e: IllegalStateException) {
                // expected
            }
        }

    @Test
    fun `GIVEN initialize called twice WHEN second initialize THEN throws`() =
        runTest {
            val dispatcher = DefaultIntentionDispatcher<TestState>(features = emptySet())
            val testDispatcher = StandardTestDispatcher(testScheduler)

            dispatcher.initialize(this, testDispatcher)

            try {
                dispatcher.initialize(this, testDispatcher)
                fail("Expected IllegalStateException when initializing twice")
            } catch (e: IllegalStateException) {
                // expected
            }
        }

    @Test
    fun `GIVEN a FlowUnitFeature WHEN dispatching THEN feature is invoked and no outcomes emitted`() =
        runTest {
            // Arrange
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val unitFeature = mockk<FlowUnitFeature<TestState>>(relaxed = true) {
                every { this@mockk.invoke(any()) } answers {
                    firstArg<Flow<Any>>().map { Unit }
                }
            }
            val dispatcher = DefaultIntentionDispatcher(features = setOf(unitFeature))

            dispatcher.initialize(this, testDispatcher)

            // When / Then
            dispatcher.observeOutcomes().test {
                dispatcher.dispatchIntention("intent")
                testDispatcher.scheduler.advanceUntilIdle()
                expectNoEvents()
            }

            verify(exactly = 1) { unitFeature.invoke(any()) }
            dispatcher.shutdown()
        }

    @Test
    fun `GIVEN multiple features WHEN dispatching THEN outcomes from all features are emitted`() =
        runTest {
            // Arrange
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val outcome1 = object : TestOutcome {}
            val outcome2 = object : TestOutcome {}
            val feature1 = mockk<FlowFeature<TestState>> {
                every { this@mockk.invoke(any()) } answers {
                    firstArg<Flow<Any>>().map { outcome1 }
                }
            }
            val feature2 = mockk<FlowFeature<TestState>> {
                every { this@mockk.invoke(any()) } answers {
                    firstArg<Flow<Any>>().map { outcome2 }
                }
            }
            val dispatcher = DefaultIntentionDispatcher<TestState>(features = setOf(feature1, feature2))

            dispatcher.initialize(this, testDispatcher)

            // When / Then
            dispatcher.observeOutcomes().test {
                dispatcher.dispatchIntention("i")
                testDispatcher.scheduler.advanceUntilIdle()
                // Collect two outcomes in any order
                val items = listOf(awaitItem(), awaitItem())
                assert(items.contains(outcome1) && items.contains(outcome2))
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { feature1.invoke(any()) }
            verify(exactly = 1) { feature2.invoke(any()) }
            dispatcher.shutdown()
        }

    @Test
    fun `GIVEN two collectors WHEN dispatching THEN both collectors receive outcome`() =
        runTest {
            // Arrange
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val outcome = object : TestOutcome {}
            val feature = mockk<FlowFeature<TestState>> {
                every { this@mockk.invoke(any()) } answers {
                    firstArg<Flow<Any>>().map { outcome }
                }
            }
            val dispatcher = DefaultIntentionDispatcher(features = setOf(feature))

            dispatcher.initialize(this, testDispatcher)

            // Start two concurrent collectors
            val job1 = launch {
                dispatcher.observeOutcomes().test {
                    assertEquals(outcome, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            }
            val job2 = launch {
                dispatcher.observeOutcomes().test {
                    assertEquals(outcome, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            }

            // Ensure both collectors have actually started collecting before dispatching
            testDispatcher.scheduler.advanceUntilIdle()

            // When
            dispatcher.dispatchIntention("i")
            testDispatcher.scheduler.advanceUntilIdle()

            job1.join()
            job2.join()

            verify(exactly = 1) { feature.invoke(any()) }
            dispatcher.shutdown()
        }
 }
