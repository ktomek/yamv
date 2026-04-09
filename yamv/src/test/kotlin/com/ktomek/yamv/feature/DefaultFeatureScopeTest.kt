package com.ktomek.yamv.feature

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig
import com.ktomek.yamv.state.MviRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

private data class ScopedFeatureState(val ticks: Int = 0) : State

class DefaultFeatureScopeTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `DefaultFeatureScope creates an active scope`() {
        val scope = DefaultFeatureScope()
        assertThat(scope.featureScope.isActive).isTrue()
    }

    @Test
    fun `DefaultFeatureScope with custom dispatcher creates an active scope`() = runTest(dispatcher) {
        val scope = DefaultFeatureScope(dispatcher)
        assertThat(scope.featureScope.isActive).isTrue()
    }

    @Test
    fun `featureScope launches background coroutines that emit outcomes`() = runTest(dispatcher) {
        val feature = object : Feature.FlowFeature<ScopedFeatureState>, HasFeatureScope by DefaultFeatureScope(dispatcher) {
            override fun invoke(intentions: Flow<Any>): Flow<Outcome<ScopedFeatureState>> = channelFlow {
                featureScope.launch {
                    repeat(3) {
                        delay(50)
                        send(StateOutcome { state -> state.copy(ticks = state.ticks + 1) })
                    }
                }
                intentions.collect { }
            }
        }

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = ScopedFeatureState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        testScheduler.advanceTimeBy(200)

        assertThat(runtime.state.value.ticks).isEqualTo(3)
        runtime.clear()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `featureScope is cancelled when MviRuntime is cleared`() = runTest(dispatcher) {
        val featureScopeDelegate = DefaultFeatureScope(dispatcher)

        val feature = object : Feature.FlowFeature<ScopedFeatureState>, HasFeatureScope by featureScopeDelegate {
            override fun invoke(intentions: Flow<Any>): Flow<Outcome<ScopedFeatureState>> = channelFlow {
                featureScope.launch { delay(Long.MAX_VALUE) }
                intentions.collect { }
            }
        }

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = ScopedFeatureState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        assertThat(featureScopeDelegate.featureScope.isActive).isTrue()

        runtime.clear()
        testScheduler.advanceUntilIdle()

        assertThat(featureScopeDelegate.featureScope.isActive).isFalse()
    }

    @Test
    fun `features without HasFeatureScope are unaffected by shutdown`() = runTest(dispatcher) {
        val feature = object : Feature.FlowFeature<ScopedFeatureState> {
            override fun invoke(intentions: Flow<Any>): Flow<Outcome<ScopedFeatureState>> = channelFlow {
                intentions.collect { }
            }
        }

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = ScopedFeatureState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        runtime.clear()
        testScheduler.advanceUntilIdle()
        // No exception = pass
    }
}
