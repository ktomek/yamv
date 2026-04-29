package com.ktomek.yamv.feature

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.State
import com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig
import com.ktomek.yamv.state.MviRuntime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

private data class UnitTestState(val count: Int = 0) : State
private data class TypedDoAction(val id: String)
private data class TypedOtherAction(val id: String)

@OptIn(ExperimentalCoroutinesApi::class)
class TypedUnitFeatureWrapperTest {

    @Test
    fun `wrap produces a FlowUnitFeature`() {
        val feature = TypedUnitFeature<UnitTestState, TypedDoAction> { intentions ->
            intentions.map { }
        }.wrap()
        assertThat(feature).isInstanceOf(Feature.FlowUnitFeature::class.java)
        assertThat(feature).isNotInstanceOf(Feature.FlowFeature::class.java)
    }

    @Test
    fun `matching intentions are passed to the typed flow`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val received = mutableListOf<String>()

        val feature = TypedUnitFeature<UnitTestState, TypedDoAction> { intentions ->
            intentions.onEach { received.add(it.id) }.map { }
        }.wrap()

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = UnitTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        runtime.dispatch(TypedDoAction("a"))
        runtime.dispatch(TypedDoAction("b"))
        testScheduler.advanceUntilIdle()

        assertThat(received).containsExactly("a", "b").inOrder()
        runtime.clear()
    }

    @Test
    fun `non-matching intentions are filtered out`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val received = mutableListOf<String>()

        val feature = TypedUnitFeature<UnitTestState, TypedDoAction> { intentions ->
            intentions.onEach { received.add(it.id) }.map { }
        }.wrap()

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = UnitTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        runtime.dispatch(TypedOtherAction("ignored"))
        runtime.dispatch(TypedDoAction("kept"))
        testScheduler.advanceUntilIdle()

        assertThat(received).containsExactly("kept")
        runtime.clear()
    }

    @Test
    fun `state is unchanged because no Outcome is emitted`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val feature = TypedUnitFeature<UnitTestState, TypedDoAction> { intentions ->
            intentions.map { }
        }.wrap()

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = UnitTestState(count = 7),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        runtime.dispatch(TypedDoAction("a"))
        testScheduler.advanceUntilIdle()

        assertThat(runtime.state.value.count).isEqualTo(7)
        runtime.clear()
    }
}
