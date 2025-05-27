package com.ktomek.yamv.state

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.IntentionOutcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.intention.IntentionDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StateContainer<S : State>(
    private val intentionDispatcher: IntentionDispatcher<S>,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val defaultState: S,
) {

    private val effectsFlow: MutableSharedFlow<EffectOutcome<S>> = MutableSharedFlow()
    val effects: Flow<EffectOutcome<S>>
        get() = effectsFlow

    private val stateFlow: MutableStateFlow<S> = MutableStateFlow(defaultState)
    val state: StateFlow<S>
        get() = stateFlow

    init {
        intentionDispatcher
        scope.launch(dispatcher) {
            intentionDispatcher
                .observeOutcomes()
                .filterIsInstance<StateOutcome<S>>()
                .scan(defaultState) { state, outcome -> outcome.reduce(state) }
                .collect { state -> stateFlow.update { state } }
        }

        scope.launch(dispatcher) {
            intentionDispatcher
                .observeOutcomes()
                .filterIsInstance<EffectOutcome<S>>()
                .collect(effectsFlow::emit)
        }

        scope.launch(dispatcher) {
            intentionDispatcher
                .observeOutcomes()
                .filterIsInstance<IntentionOutcome<S>>()
                .map { it.intention }
                .collect(intentionDispatcher::dispatchIntention)
        }
    }

    fun dispatchIntention(intention: Any) {
        scope.launch {
            intentionDispatcher.dispatchIntention(intention)
        }
    }

    fun close() = Unit
}
