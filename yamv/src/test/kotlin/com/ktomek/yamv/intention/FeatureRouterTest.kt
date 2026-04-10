package com.ktomek.yamv.intention

import app.cash.turbine.test
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.Feature.FlowFeature
import com.ktomek.yamv.feature.Feature.FlowUnitFeature
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
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

private fun testDispatcherConfig(dispatcher: CoroutineDispatcher) = object : CoroutineDispatcherConfig {
    override fun provideIntentionDispatcher(intention: Any?) = dispatcher
    override fun provideReducerDispatcher() = dispatcher
    override fun provideFeatureDispatcher(feature: Any) = dispatcher
}

class FeatureRouterTest {

    @Test
    fun `GIVEN a router with no features WHEN dispatching intentions THEN no outcomes are emitted`() =
        runTest {
            // Arrange — create a router tied to this test scheduler
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val router = FeatureRouter<TestState>(
                features = emptySet()
            )

            router.initialize(this, testDispatcherConfig(testDispatcher))

            // When / Then
            router.observeOutcomes().test {
                expectNoEvents()
            }
            router.dispatchIntention("any")
            testDispatcher.scheduler.advanceUntilIdle()
            router.observeOutcomes().test {
                expectNoEvents()
            }
        }

    @Test
    fun `GIVEN a router with one feature WHEN dispatching an intention THEN the feature processes it and emits an outcome`() =
        runTest {
            // Arrange
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val outcome = object : TestOutcome {}
            val feature = mockk<FlowFeature<TestState>> {
                every { this@mockk.invoke(any()) } answers {
                    firstArg<Flow<Any>>().map { outcome }
                }
            }
            val router = FeatureRouter(
                features = setOf<Feature<TestState>>(feature)
            )

            router.initialize(this, testDispatcherConfig(testDispatcher))
            testDispatcher.scheduler.advanceUntilIdle()

            // When & Then
            router.observeOutcomes().test {
                router.dispatchIntention("first")
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(outcome, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { feature.invoke(any()) }
            // Cancel feature coroutines launched by router to avoid leaked coroutines after runTest
            router.shutdown()
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
            val router = FeatureRouter(
                features = setOf<Feature<TestState>>(feature)
            )

            router.initialize(this, testDispatcherConfig(testDispatcher))

            // When
            router.dispatchIntention("first")
            router.dispatchIntention("second")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            verify(exactly = 1) { feature.invoke(any()) }
            router.shutdown()
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
            val router = FeatureRouter(
                features = setOf<Feature<TestState>>(feature)
            )

            router.initialize(this, testDispatcherConfig(testDispatcher))

            // When & Then
            router.observeOutcomes().test {
                router.dispatchIntention("something")
                testDispatcher.scheduler.advanceUntilIdle()
                assertEquals(outcome, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { feature.invoke(any()) }
            router.shutdown()
        }

    @Test
    fun `GIVEN dispatchIntention called before initialize WHEN dispatching THEN throws`() =
        runTest {
            // Arrange
            val router = FeatureRouter<TestState>(features = emptySet())

            // Act / Assert
            try {
                router.dispatchIntention("x")
                fail("Expected IllegalStateException when dispatching before initialize")
            } catch (ignored: IllegalStateException) {
                // expected
            }
        }

    @Test
    fun `GIVEN observeOutcomes called before initialize WHEN observing THEN throws`() =
        runTest {
            // Arrange
            val router = FeatureRouter<TestState>(features = emptySet())

            // Act / Assert
            try {
                router.observeOutcomes()
                fail("Expected IllegalStateException when observing before initialize")
            } catch (ignored: IllegalStateException) {
                // expected
            }
        }

    @Test
    fun `GIVEN initialize called twice WHEN second initialize THEN throws`() =
        runTest {
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val router = FeatureRouter<TestState>(features = emptySet())

            router.initialize(this, testDispatcherConfig(testDispatcher))

            try {
                router.initialize(this, testDispatcherConfig(testDispatcher))
                fail("Expected IllegalStateException when initializing twice")
            } catch (ignored: IllegalStateException) {
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
            val router = FeatureRouter(features = setOf(unitFeature))

            router.initialize(this, testDispatcherConfig(testDispatcher))

            // When / Then
            router.observeOutcomes().test {
                router.dispatchIntention("intent")
                testDispatcher.scheduler.advanceUntilIdle()
                expectNoEvents()
            }

            verify(exactly = 1) { unitFeature.invoke(any()) }
            router.shutdown()
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
            val router = FeatureRouter<TestState>(features = setOf(feature1, feature2))

            router.initialize(this, testDispatcherConfig(testDispatcher))

            // When / Then
            router.observeOutcomes().test {
                router.dispatchIntention("i")
                testDispatcher.scheduler.advanceUntilIdle()
                // Collect two outcomes in any order
                val items = listOf(awaitItem(), awaitItem())
                assert(items.contains(outcome1) && items.contains(outcome2))
                cancelAndIgnoreRemainingEvents()
            }
            verify(exactly = 1) { feature1.invoke(any()) }
            verify(exactly = 1) { feature2.invoke(any()) }
            router.shutdown()
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
            val router = FeatureRouter(features = setOf(feature))

            router.initialize(this, testDispatcherConfig(testDispatcher))

            // Start two concurrent collectors
            val job1 = launch {
                router.observeOutcomes().test {
                    assertEquals(outcome, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            }
            val job2 = launch {
                router.observeOutcomes().test {
                    assertEquals(outcome, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            }

            // Ensure both collectors have actually started collecting before dispatching
            testDispatcher.scheduler.advanceUntilIdle()

            // When
            router.dispatchIntention("i")
            testDispatcher.scheduler.advanceUntilIdle()

            job1.join()
            job2.join()

            verify(exactly = 1) { feature.invoke(any()) }
            router.shutdown()
        }
}
