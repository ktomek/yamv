package com.ktomek.yamv.state

import com.google.common.truth.Truth.assertThat
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.core.UiMappable
import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.feature.wrap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

private data class FullState(
    val count: Int = 0,
    val internalRetryCount: Int = 0,
    val paginationCursor: String? = null,
) : State, UiMappable<TestUiState> {
    override fun toUiState() = TestUiState(displayCount = count.toString())
}

private data class TestUiState(val displayCount: String)

private sealed class TestIntention {
    data object Increment : TestIntention()
}

@OptIn(ExperimentalCoroutinesApi::class)
class UiStateMappingTest {

    @Test
    fun `mapToUiState produces correct initial value`() = runTest(UnconfinedTestDispatcher()) {
        val state = MutableStateFlow(FullState(count = 42))
        val uiState = state.mapToUiState(backgroundScope, SharingStarted.Eagerly) { it.toUiState() }

        assertThat(uiState.value.displayCount).isEqualTo("42")
    }

    @Test
    fun `mapToUiState emits only when UiState changes`() = runTest(UnconfinedTestDispatcher()) {
        val state = MutableStateFlow(FullState())
        val emissions = mutableListOf<TestUiState>()

        val uiState = state.mapToUiState(backgroundScope, SharingStarted.Eagerly) { it.toUiState() }
        val job = backgroundScope.launch { uiState.collect { emissions.add(it) } }

        // Change internal field only — UiState should NOT change
        state.value = state.value.copy(internalRetryCount = 5)
        state.value = state.value.copy(paginationCursor = "abc")

        // Change count — UiState SHOULD change
        state.value = state.value.copy(count = 1)

        // Initial "0" + updated "1" = 2 distinct emissions
        assertThat(emissions.map { it.displayCount }).containsExactly("0", "1").inOrder()

        job.cancel()
    }

    @Test
    fun `uiState extension works with UiMappable interface`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val feature = FunctionTypedFeature<FullState, TestIntention.Increment> {
            StateOutcome { s -> s.copy(count = s.count + 1) }
        }
        val config = object : CoroutineDispatcherConfig {
            override fun provideIntentionDispatcher(intention: Any?) = testDispatcher
            override fun provideReducerDispatcher() = testDispatcher
            override fun provideFeatureDispatcher(feature: Any) = testDispatcher
        }

        val runtime = MviRuntime(
            features = setOf(feature.wrap()),
            defaultState = FullState(),
            dispatcherConfig = config,
        )

        val uiState = runtime.uiState<FullState, TestUiState>(backgroundScope, SharingStarted.Eagerly)
        advanceUntilIdle()

        assertThat(uiState.value.displayCount).isEqualTo("0")

        runtime.dispatch(TestIntention.Increment)
        advanceUntilIdle()

        assertThat(uiState.value.displayCount).isEqualTo("1")
        runtime.clear()
    }

    @Test
    fun `mapToUiState with custom mapper`() = runTest(UnconfinedTestDispatcher()) {
        val state = MutableStateFlow(FullState(count = 7))

        val uiState = state.mapToUiState(backgroundScope, SharingStarted.Eagerly) { s ->
            TestUiState(displayCount = "Count: ${s.count}")
        }

        assertThat(uiState.value.displayCount).isEqualTo("Count: 7")

        state.value = state.value.copy(count = 99)

        assertThat(uiState.value.displayCount).isEqualTo("Count: 99")
    }
}
