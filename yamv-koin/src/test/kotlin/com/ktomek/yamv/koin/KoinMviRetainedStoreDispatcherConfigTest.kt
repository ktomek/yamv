package com.ktomek.yamv.koin

import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

private data class TestState(val count: Int = 0) : State

private class TrackingCoroutineDispatcherConfig(
    private val delegate: CoroutineDispatcher = Dispatchers.Default,
) : CoroutineDispatcherConfig {
    var intentionDispatcherCallCount = 0
    var reducerDispatcherCallCount = 0
    var featureDispatcherCallCount = 0

    override fun provideIntentionDispatcher(intention: Any?): CoroutineDispatcher {
        intentionDispatcherCallCount++
        return delegate
    }

    override fun provideReducerDispatcher(): CoroutineDispatcher {
        reducerDispatcherCallCount++
        return delegate
    }

    override fun provideFeatureDispatcher(feature: Any): CoroutineDispatcher {
        featureDispatcherCallCount++
        return delegate
    }
}

/**
 * Subclass of [KoinMviRetainedStore] that exposes [dispatcherConfig] for assertion in tests.
 */
private class InspectableMviRetainedStore<S : State>(
    features: Set<Feature<S>>,
    defaultState: S,
    config: CoroutineDispatcherConfig,
) : KoinMviRetainedStore<S>(
    features = features,
    defaultState = defaultState,
    dispatcherConfig = config,
) {
    fun getDispatcherConfig(): CoroutineDispatcherConfig = dispatcherConfig
}

class KoinMviRetainedStoreDispatcherConfigTest {

    @Test
    fun `GIVEN KoinMviRetainedStore with default config WHEN created THEN dispatcherConfig is DefaultCoroutineDispatcherConfig`() {
        // Arrange & Act
        val store = InspectableMviRetainedStore(
            features = emptySet<Feature<TestState>>(),
            defaultState = TestState(),
            config = DefaultCoroutineDispatcherConfig(),
        )

        // Assert
        assertTrue(store.getDispatcherConfig() is DefaultCoroutineDispatcherConfig)
    }

    @Test
    fun `GIVEN KoinMviRetainedStore with custom config WHEN created THEN dispatcherConfig is the custom instance`() {
        // Arrange
        val customConfig = TrackingCoroutineDispatcherConfig()

        // Act
        val store = InspectableMviRetainedStore(
            features = emptySet<Feature<TestState>>(),
            defaultState = TestState(),
            config = customConfig,
        )

        // Assert
        assertSame(customConfig, store.getDispatcherConfig())
    }

    @Test
    fun `GIVEN KoinMviRetainedStore with custom config WHEN store is created THEN custom config dispatchers are invoked`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val customConfig = TrackingCoroutineDispatcherConfig(delegate = testDispatcher)

        // Act
        InspectableMviRetainedStore(
            features = emptySet<Feature<TestState>>(),
            defaultState = TestState(),
            config = customConfig,
        )

        // Assert — MviRuntime calls provideReducerDispatcher on init
        assertTrue(
            customConfig.reducerDispatcherCallCount > 0,
            "Expected reducerDispatcherCallCount > 0 but was ${customConfig.reducerDispatcherCallCount}",
        )
    }

    @Test
    fun `GIVEN KoinMviRetainedStore with custom config WHEN intention dispatched THEN custom intention dispatcher is used`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val customConfig = TrackingCoroutineDispatcherConfig(delegate = testDispatcher)
        val store = InspectableMviRetainedStore(
            features = emptySet<Feature<TestState>>(),
            defaultState = TestState(),
            config = customConfig,
        )
        val callCountBeforeDispatch = customConfig.intentionDispatcherCallCount

        // Act
        store.dispatch("increment")
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(
            callCountBeforeDispatch + 1,
            customConfig.intentionDispatcherCallCount,
            "Expected intentionDispatcherCallCount to increase by 1",
        )
    }
}
