package com.ktomek.yamv.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

abstract class ViewModelStateStore<S : State, R : Outcome<S>, E : EffectOutcome<S>>(
    stateContainerFactory: StateContainerFactory<S, R, E>,
) : ViewModel(), StateStore<S, E> {

    private val stateContainer: StateContainer<S, R, E> =
        stateContainerFactory.create(viewModelScope)
    override val state: StateFlow<S>
        get() = stateContainer.state

    override val effects: Flow<E> = stateContainer.effects

    override fun dispatch(intention: Any) {
        stateContainer.dispatchIntention(intention)
    }

    override operator fun invoke(intention: Any) {
        dispatch(intention)
    }

    override fun onCleared() {
        stateContainer.close()
    }
}
