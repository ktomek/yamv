package com.ktomek.yamv.viewmodel

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.state.MviStore
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

private interface TestState : State {
    val value: String
}

private data class TestStateImpl(override val value: String = "initial") : TestState

private class TestMviRetainedStore(override val store: MviStore<TestState, Any>) :
    MviRetainedStore<TestState, Any>()

class MviRetainedStoreTest {

    @Test
    fun `GIVEN MviViewModel with store WHEN accessing state THEN delegates to store state`() {
        // Arrange
        val stateFlow = MutableStateFlow<TestState>(TestStateImpl("test"))
        val mockStore: MviStore<TestState, Any> = mockk(relaxed = true)
        io.mockk.every { mockStore.state } returns stateFlow
        val viewModel = TestMviRetainedStore(mockStore)

        // Act & Assert
        assertEquals(stateFlow, viewModel.state)
        verify { mockStore.state }
    }

    @Test
    fun `GIVEN MviViewModel with store WHEN accessing effects THEN delegates to store effects`() {
        // Arrange
        val effectsFlow: Flow<EffectOutcome<TestState>> = emptyFlow()
        val mockStore: MviStore<TestState, Any> = mockk(relaxed = true)
        io.mockk.every { mockStore.effects } returns effectsFlow
        val viewModel = TestMviRetainedStore(mockStore)

        // Act & Assert
        assertEquals(effectsFlow, viewModel.effects)
        verify { mockStore.effects }
    }

    @Test
    fun `GIVEN MviViewModel with store WHEN dispatch is called THEN delegates to store dispatch`() {
        // Arrange
        val intention = "test-intention"
        val mockStore: MviStore<TestState, Any> = mockk(relaxed = true)
        val viewModel = TestMviRetainedStore(mockStore)

        // Act
        viewModel.dispatch(intention)

        // Assert
        verify { mockStore.dispatch(intention) }
    }

    @Test
    fun `GIVEN MviViewModel with store WHEN clear is called THEN delegates to store clear`() {
        // Arrange
        val mockStore: MviStore<TestState, Any> = mockk(relaxed = true)
        val viewModel = TestMviRetainedStore(mockStore)

        // Act
        viewModel.clear()

        // Assert
        verify { mockStore.clear() }
    }
}
