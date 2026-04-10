package com.ktomek.yamv.koin

import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.module
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
 * Subclass of [KoinMviRetainedStore] used in tests that require construction via a named config parameter.
 *
 * Exposes [dispatcherConfig] publicly so tests can assert on it, since the base class keeps it protected.
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
    public override val dispatcherConfig: CoroutineDispatcherConfig
        get() = super.dispatcherConfig
}

class KoinMviRetainedStoreDispatcherConfigTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `GIVEN KoinMviRetainedStore with default config WHEN created THEN dispatcherConfig is DefaultCoroutineDispatcherConfig`() {
        // Arrange & Act
        val store = InspectableMviRetainedStore(
            features = emptySet<Feature<TestState>>(),
            defaultState = TestState(),
            config = DefaultCoroutineDispatcherConfig(),
        )

        // Assert
        assertTrue(store.dispatcherConfig is DefaultCoroutineDispatcherConfig)
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
        assertSame(customConfig, store.dispatcherConfig)
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

    @Test
    fun `GIVEN mviStore DSL with custom dispatcherConfig WHEN resolved via Koin THEN store uses custom dispatcherConfig`() {
        // Arrange
        val customConfig = TrackingCoroutineDispatcherConfig()
        val koinModule = module {
            mviStore(defaultState = TestState(), dispatcherConfig = customConfig) {}
        }

        // Act
        val koinApp = KoinApplication.init().modules(koinModule)
        koinApp.koin.get<KoinMviRetainedStore<TestState>>(stateQualifier<TestState>())

        // Assert — MviRuntime calls provideReducerDispatcher on init using the custom config
        assertTrue(
            customConfig.reducerDispatcherCallCount > 0,
            "Expected reducerDispatcherCallCount > 0 but was ${customConfig.reducerDispatcherCallCount}",
        )
    }
}
