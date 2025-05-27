package com.ktomek.yamv.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.intention.DefaultIntentionDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstract class representing a ViewModel-based state store.
 *
 * @param S The type of the state.
 * @param stateContainerFactory The factory to create the state container.
 */
open class StateContainerHost<S : State>(
    private val features: Set<Feature<S>>,
    private val defaultState: S,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(), StateStore<S> {

    private val stateContainer: StateContainer<S> = StateContainer(
        intentionDispatcher = DefaultIntentionDispatcher<S>(
            features = features,
            scope = viewModelScope,
            dispatcher = dispatcher
        ),
        scope = viewModelScope,
        defaultState = defaultState
    )

    /**
     * The current state as a [StateFlow].
     */
    override val state: StateFlow<S>
        get() = stateContainer.state

    /**
     * The effects as a [Flow].
     */
    override val effects: Flow<EffectOutcome<S>> = stateContainer.effects

    /**
     * Dispatches an intention.
     *
     * @param intention The intention to be dispatched.
     */
    override fun dispatch(intention: Any) {
        stateContainer.dispatchIntention(intention)
    }

    /**
     * Called when the ViewModel is cleared.
     * Closes the state container.
     */
    override fun onCleared() {
        stateContainer.close()
    }
}
