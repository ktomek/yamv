package com.ktomek.yamv.state

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.IntentionOutcome
import com.ktomek.yamv.core.Reducer
import com.ktomek.yamv.core.State
import com.ktomek.yamv.intention.IntentionDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface YamvDispatcherProvider {
    fun providerIntentionDispatcher(intention: Any?): CoroutineDispatcher
    fun providerReducerDispatcher(): CoroutineDispatcher
    fun provideFeatureDispatcher(feature: Any): CoroutineDispatcher
}

class DefaultDispatcherProvider constructor(
    private val default: CoroutineDispatcher = Dispatchers.Default,
    private val ui: CoroutineDispatcher = Dispatchers.Main,
): YamvDispatcherProvider {
    override fun providerIntentionDispatcher(intention: Any?): CoroutineDispatcher = ui

    override fun providerReducerDispatcher(): CoroutineDispatcher = ui

    override fun provideFeatureDispatcher(feature: Any): CoroutineDispatcher = default
}

internal class StateContainer<S : State>(
    private val intentionDispatcher: IntentionDispatcher<S>,
    private val dispatcherProvider: YamvDispatcherProvider,
    val defaultState: S,
) {

    private val scope = CoroutineScope(dispatcherProvider.providerReducerDispatcher())

    private val effectsFlow: MutableSharedFlow<EffectOutcome<S>> = MutableSharedFlow()
    val effects: Flow<EffectOutcome<S>>
        get() = effectsFlow

    private val stateFlow: MutableStateFlow<S> = MutableStateFlow(defaultState)
    val state: StateFlow<S>
        get() = stateFlow

    init {
        // Start observing outcomes and split them into reducers, effects and intentions
        intentionDispatcher.initialize(scope, dispatcherProvider)

        scope.launch(dispatcherProvider.providerReducerDispatcher()) {
            intentionDispatcher
                .observeOutcomes()
                .filterIsInstance<Reducer<S>>()
                .scan(defaultState) { state, reducer -> reducer.reduce(state) }
                .collect { newState -> stateFlow.update { newState } }
        }

        scope.launch(dispatcherProvider.providerReducerDispatcher()) {
            intentionDispatcher
                .observeOutcomes()
                .filterIsInstance<EffectOutcome<S>>()
                .collect(effectsFlow::emit)
        }

        scope.launch(dispatcherProvider.providerIntentionDispatcher(null)) {
            intentionDispatcher
                .observeOutcomes()
                .filterIsInstance<IntentionOutcome<S>>()
                .map { it.intention }
                .collect(intentionDispatcher::dispatchIntention)
        }
    }

    fun dispatchIntention(intention: Any) {
        scope.launch(dispatcherProvider.providerIntentionDispatcher(intention)) {
            intentionDispatcher.dispatchIntention(intention)
        }
    }

    fun close() {
        scope.cancel()
    }
}
