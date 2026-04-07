package com.ktomek.yamv.koin.counter

import androidx.lifecycle.ViewModel
import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.ui.counter.logic.state.CounterState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class CounterViewModel(private val store: CounterStateStore) : ViewModel() {
    val state: StateFlow<CounterState> = store.store.state
    val effects: Flow<EffectOutcome<CounterState>> = store.store.effects
    fun dispatch(intention: Any) = store.store.dispatch(intention)
    override fun onCleared() = store.store.clear()
}
