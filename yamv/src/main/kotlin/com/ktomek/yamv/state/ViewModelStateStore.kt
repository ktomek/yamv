package com.ktomek.yamv.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstract class representing a ViewModel-based state store.
 *
 * @param S The type of the state.
 * @param O The type of the outcome that produces the new state.
 * @param E The type of the effect outcome.
 * @param stateContainerFactory The factory to create the state container.
 */
abstract class ViewModelStateStore<S : State, O : Outcome<S>, E : EffectOutcome<S>>(
    stateContainerFactory: StateContainerFactory<S, O, E>,
) : ViewModel(), StateStore<S, E> {

    private val stateContainer: StateContainer<S, O, E> =
        stateContainerFactory.create(viewModelScope)

    /**
     * The current state as a [StateFlow].
     */
    override val state: StateFlow<S>
        get() = stateContainer.state

    /**
     * The effects as a [Flow].
     */
    override val effects: Flow<E> = stateContainer.effects

    /**
     * Dispatches an intention.
     *
     * @param intention The intention to be dispatched.
     */
    override fun dispatch(intention: Any) {
        stateContainer.dispatchIntention(intention)
    }

    /**
     * Invokes the store with an intention.
     *
     * @param intention The intention to be invoked.
     */
    override operator fun invoke(intention: Any) {
        dispatch(intention)
    }

    /**
     * Called when the ViewModel is cleared.
     * Closes the state container.
     */
    override fun onCleared() {
        stateContainer.close()
    }
}
