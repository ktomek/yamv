package com.ktomek.yamv.intention

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.DefaultFeatureScope
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.HasFeatureDispatcher
import com.ktomek.yamv.feature.HasFeatureScope
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.coroutines.ContinuationInterceptor

private data class DispatcherTestState(val dispatcher: String = "") : State

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureRouterDispatcherTest {

    @Test
    fun `feature with HasFeatureDispatcher runs on feature dispatcher`() = runTest {
        val featureDispatcher = StandardTestDispatcher(testScheduler, name = "feature")
        val configDispatcher = StandardTestDispatcher(testScheduler, name = "config")
        var capturedDispatcher: CoroutineDispatcher? = null

        val feature = object : Feature.FlowFeature<DispatcherTestState>, HasFeatureDispatcher {
            override val featureDispatcher = featureDispatcher
            override fun invoke(intentions: Flow<Any>): Flow<Outcome<DispatcherTestState>> =
                channelFlow {
                    capturedDispatcher =
                        coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
                    intentions.collect { send(StateOutcome { it }) }
                }
        }

        val router = FeatureRouter(features = setOf(feature))
        router.initialize(this, testDispatcherConfig(configDispatcher))
        testScheduler.advanceUntilIdle()

        assertThat(capturedDispatcher).isSameInstanceAs(featureDispatcher)
        router.shutdown()
    }

    @Test
    fun `feature with HasFeatureScope runs on scope dispatcher`() = runTest {
        val scopeDispatcher = StandardTestDispatcher(testScheduler, name = "scope")
        val configDispatcher = StandardTestDispatcher(testScheduler, name = "config")
        var capturedDispatcher: CoroutineDispatcher? = null

        val feature = object :
            Feature.FlowFeature<DispatcherTestState>,
            HasFeatureScope by DefaultFeatureScope(scopeDispatcher) {
            override fun invoke(intentions: Flow<Any>): Flow<Outcome<DispatcherTestState>> =
                channelFlow {
                    capturedDispatcher =
                        coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
                    intentions.collect { send(StateOutcome { it }) }
                }
        }

        val router = FeatureRouter(features = setOf(feature))
        router.initialize(this, testDispatcherConfig(configDispatcher))
        testScheduler.advanceUntilIdle()

        assertThat(capturedDispatcher).isSameInstanceAs(scopeDispatcher)
        router.shutdown()
    }

    @Test
    fun `feature without HasFeatureDispatcher falls back to config`() = runTest {
        val configDispatcher = StandardTestDispatcher(testScheduler, name = "config")
        var capturedDispatcher: CoroutineDispatcher? = null

        val feature = Feature.FlowFeature<DispatcherTestState> { intentions ->
            channelFlow {
                capturedDispatcher =
                    coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
                intentions.collect { send(StateOutcome { it }) }
            }
        }

        val router = FeatureRouter(features = setOf(feature))
        router.initialize(this, testDispatcherConfig(configDispatcher))
        testScheduler.advanceUntilIdle()

        assertThat(capturedDispatcher).isSameInstanceAs(configDispatcher)
        router.shutdown()
    }
}

private fun testDispatcherConfig(dispatcher: CoroutineDispatcher) =
    object : CoroutineDispatcherConfig {
        override fun provideIntentionDispatcher(intention: Any?) = dispatcher
        override fun provideReducerDispatcher() = dispatcher
        override fun provideFeatureDispatcher(feature: Any) = dispatcher
    }
