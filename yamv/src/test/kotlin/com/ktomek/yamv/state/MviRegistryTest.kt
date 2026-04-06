package com.ktomek.yamv.state

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.Feature.FlowFeature
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private interface TestState1 : State {
    val value: String
}

private data class TestState1Impl(override val value: String = "state1") : TestState1

private interface TestState2 : State {
    val count: Int
}

private data class TestState2Impl(override val count: Int = 0) : TestState2

private fun testDispatcherConfig(dispatcher: CoroutineDispatcher) = object : CoroutineDispatcherConfig {
    override fun provideIntentionDispatcher(intention: Any?) = dispatcher
    override fun provideReducerDispatcher() = dispatcher
    override fun provideFeatureDispatcher(feature: Any) = dispatcher
}

class MviRegistryTest {

    @Test
    fun `GIVEN DefaultMviRegistry WHEN runtime registered THEN observeStates returns correct flow`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val defaultState = TestState1Impl(value = "initial")
        val runtime = MviRuntime(
            features = emptySet(),
            defaultState = defaultState,
            dispatcherConfig = testDispatcherConfig(testDispatcher)
        )
        val registry = DefaultMviRegistry()

        // Act
        registry.register(runtime)

        // Assert
        val observedState = registry.observeStates(TestState1Impl::class.java)
        assertEquals(defaultState, observedState.value)
    }

    @Test
    fun `GIVEN DefaultMviRegistry WHEN runtime unregistered THEN observeStates throws`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val defaultState = TestState1Impl(value = "initial")
        val runtime = MviRuntime(
            features = emptySet(),
            defaultState = defaultState,
            dispatcherConfig = testDispatcherConfig(testDispatcher)
        )
        val registry = DefaultMviRegistry()
        registry.register(runtime)

        // Act & Assert
        registry.unregister(runtime)
        assertFailsWith<IllegalStateException> {
            registry.observeStates(TestState1Impl::class.java)
        }
    }

    @Test
    fun `GIVEN DefaultMviRegistry with registered runtime WHEN dispatch called THEN intention reaches runtime`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val defaultState = TestState1Impl(value = "initial")
        var dispatchedIntention: Any? = null

        val feature = object : FlowFeature<TestState1> {
            override fun invoke(intentions: Flow<Any>): Flow<Outcome<TestState1>> {
                return intentions.map { intention ->
                    dispatchedIntention = intention
                    StateOutcome { prev -> prev }
                }
            }
        }

        val runtime = MviRuntime(
            features = setOf<Feature<TestState1>>(feature),
            defaultState = defaultState,
            dispatcherConfig = testDispatcherConfig(testDispatcher)
        )
        val registry = DefaultMviRegistry()
        registry.register(runtime)

        // Act
        registry.dispatch("test_intention")
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assertEquals("test_intention", dispatchedIntention)
    }
}
