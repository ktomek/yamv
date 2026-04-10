package com.ktomek.yamv.feature

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.State
import com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig
import com.ktomek.yamv.state.MviRuntime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

private data class ActionTestState(val count: Int = 0) : State
private data class DoAction(val id: String)
private data class OtherAction(val id: String)

@OptIn(ExperimentalCoroutinesApi::class)
class ActionTypedFeatureWrapperTest {

    @Test
    fun `action is invoked when matching intention is dispatched`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val invoked = mutableListOf<String>()

        val feature = ActionTypedFeature<ActionTestState, DoAction> { intention ->
            invoked.add(intention.id)
        }.wrap()

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = ActionTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        runtime.dispatch(DoAction("a"))
        testScheduler.advanceUntilIdle()

        assertThat(invoked).containsExactly("a")
        runtime.clear()
    }

    @Test
    fun `non-matching intentions are ignored`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val invoked = mutableListOf<String>()

        val feature = ActionTypedFeature<ActionTestState, DoAction> { intention ->
            invoked.add(intention.id)
        }.wrap()

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = ActionTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        runtime.dispatch(OtherAction("ignored"))
        testScheduler.advanceUntilIdle()

        assertThat(invoked).isEmpty()
        runtime.clear()
    }

    @Test
    fun `multiple intentions each trigger the action`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val invoked = mutableListOf<String>()

        val feature = ActionTypedFeature<ActionTestState, DoAction> { intention ->
            invoked.add(intention.id)
        }.wrap()

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = ActionTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        runtime.dispatch(DoAction("1"))
        runtime.dispatch(DoAction("2"))
        runtime.dispatch(DoAction("3"))
        testScheduler.advanceUntilIdle()

        assertThat(invoked).containsExactly("1", "2", "3")
        runtime.clear()
    }

    @Test
    fun `concurrent slow actions do not block each other`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val completed = mutableListOf<String>()

        val feature = ActionTypedFeature<ActionTestState, DoAction> { intention ->
            delay(100)
            completed.add(intention.id)
        }.wrap()

        val runtime = MviRuntime(
            features = setOf(feature),
            defaultState = ActionTestState(),
            dispatcherConfig = DefaultCoroutineDispatcherConfig(dispatcher, dispatcher),
        )

        testScheduler.advanceUntilIdle()
        runtime.dispatch(DoAction("a"))
        runtime.dispatch(DoAction("b"))
        runtime.dispatch(DoAction("c"))
        testScheduler.advanceUntilIdle()

        // All 3 complete by 100ms (concurrent), not 300ms (sequential)
        testScheduler.advanceTimeBy(110)

        assertThat(completed).hasSize(3)
        runtime.clear()
    }

    @Test
    fun `wrap produces a TypedUnitFeatureHolder`() {
        val feature = ActionTypedFeature<ActionTestState, DoAction> { }.wrap()
        assertThat(feature).isInstanceOf(TypedUnitFeatureHolder::class.java)
    }
}
