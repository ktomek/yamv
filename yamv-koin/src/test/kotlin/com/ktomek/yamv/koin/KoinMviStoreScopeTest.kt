package com.ktomek.yamv.koin

import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.feature.wrap
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig
import com.ktomek.yamv.state.MviStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import kotlin.test.assertTrue

private data class TypedTestState(val count: Int = 0) : State

private sealed class TypedTestIntention {
    data object Increment : TypedTestIntention()
}

class KoinMviStoreScopeTest {

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
    fun `GIVEN typed KoinMviRetainedStore WHEN intention dispatched THEN state is reduced`() = runTest {
        // Arrange
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val feature = FunctionTypedFeature<TypedTestState, TypedTestIntention.Increment> {
            StateOutcome { s -> s.copy(count = s.count + 1) }
        }
        val store = KoinMviRetainedStore<TypedTestState, TypedTestIntention>(
            features = setOf(feature.wrap()),
            defaultState = TypedTestState(),
            dispatcherConfig = testDispatcherConfig(testDispatcher),
        )

        // Allow feature coroutines to subscribe before dispatching
        advanceUntilIdle()

        // Act — typed dispatch compiles and works
        store.dispatch(TypedTestIntention.Increment)
        advanceUntilIdle()

        // Assert
        assertEquals(1, store.state.value.count)
        store.clear()
    }

    private fun testDispatcherConfig(dispatcher: CoroutineDispatcher) =
        object : CoroutineDispatcherConfig {
            override fun provideIntentionDispatcher(intention: Any?) = dispatcher
            override fun provideReducerDispatcher() = dispatcher
            override fun provideFeatureDispatcher(feature: Any) = dispatcher
        }

    @Test
    fun `GIVEN typed mviStore DSL WHEN resolved THEN returns typed store`() {
        // Arrange
        val koinModule = module {
            mviStore<TypedTestState, TypedTestIntention>(
                defaultState = TypedTestState(),
                dispatcherConfig = DefaultCoroutineDispatcherConfig(),
            ) {}
        }

        // Act
        val koinApp = KoinApplication.init().modules(koinModule)
        val store = koinApp.koin.get<KoinMviRetainedStore<TypedTestState, TypedTestIntention>>(
            stateQualifier<TypedTestState>(),
        )

        // Assert — the store implements MviStore<TypedTestState, TypedTestIntention>
        val asInterface: MviStore<TypedTestState, TypedTestIntention> = store
        assertEquals(0, asInterface.state.value.count)
    }

    @Test
    fun `GIVEN untyped mviStore DSL WHEN resolved THEN returns untyped store accepting Any`() {
        // Arrange — Any-typed still works (explicit intention type Any)
        val koinModule = module {
            mviStore<TypedTestState, Any>(
                defaultState = TypedTestState(),
                dispatcherConfig = DefaultCoroutineDispatcherConfig(),
            ) {}
        }

        // Act
        val koinApp = KoinApplication.init().modules(koinModule)
        val store = koinApp.koin.get<KoinMviRetainedStore<TypedTestState, Any>>(
            stateQualifier<TypedTestState>(),
        )

        // Assert — Any-typed dispatch accepts anything
        assertTrue(store.state.value.count == 0)
    }

    @Test
    fun `GIVEN two separately instantiated stores WHEN same feature WHEN independent THEN state is independent`() = runTest {
        // Arrange — sanity check that manually constructed stores are independent
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val feature = FunctionTypedFeature<TypedTestState, TypedTestIntention.Increment> {
            StateOutcome { s -> s.copy(count = s.count + 1) }
        }
        val storeA = KoinMviRetainedStore<TypedTestState, TypedTestIntention>(
            features = setOf(feature.wrap()),
            defaultState = TypedTestState(),
            dispatcherConfig = testDispatcherConfig(testDispatcher),
        )
        val storeB = KoinMviRetainedStore<TypedTestState, TypedTestIntention>(
            features = setOf(feature.wrap()),
            defaultState = TypedTestState(),
            dispatcherConfig = testDispatcherConfig(testDispatcher),
        )

        // Allow feature coroutines to subscribe before dispatching
        advanceUntilIdle()

        // Act
        storeA.dispatch(TypedTestIntention.Increment)
        advanceUntilIdle()

        // Assert — storeB's state is unaffected
        assertEquals(1, storeA.state.value.count)
        assertEquals(0, storeB.state.value.count)

        storeA.clear()
        storeB.clear()
    }
}
