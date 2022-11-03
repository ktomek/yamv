package com.ktomek.yamv.state

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.IntentionOutcome
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.Reducer
import com.ktomek.yamv.core.State
import com.ktomek.yamv.intention.DefaultIntentionDispatcher
import com.ktomek.yamv.intention.GlobalIntention
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.Closeable

abstract class StateContainer<S : State, R : Outcome<S>, E : EffectOutcome<S>>(
    private val intentionDispatcher: DefaultIntentionDispatcher<R>,
    private val reducer: Reducer<S, R>,
    private val store: DefaultStore,
    private val scope: CoroutineScope
) : Closeable {

    private val effectsFlow: MutableSharedFlow<E> = MutableSharedFlow()
    val effects: Flow<E>
        get() = effectsFlow

    @Suppress("LeakingThis")
    private val stateFlow: MutableStateFlow<S> = MutableStateFlow(defaultState)
    val state: StateFlow<S>
        get() = stateFlow

    abstract val defaultState: S

    abstract val stateType: Class<S>

    init {
        @Suppress("LeakingThis")
        store.register(this)
        scope.launch(Dispatchers.Default) {
            intentionDispatcher
                .observeResults()
                .scan(defaultState, reducer::reduce)
                .drop(1)
                .collect { state -> stateFlow.update { state } }
        }

        scope.launch(Dispatchers.Default) {
            intentionDispatcher
                .observeResults()
                .filterIsInstance<EffectOutcome<S>>()
                .collect {
                    @Suppress("UNCHECKED_CAST")
                    effectsFlow.emit(it as E)
                }
        }

        scope.launch(Dispatchers.Default) {
            intentionDispatcher
                .observeResults()
                .filterIsInstance<IntentionOutcome<S>>()
                .collect {
                    if (it.intention is GlobalIntention) {
                        store.dispatch(it.intention)
                    } else {
                        intentionDispatcher.dispatchIntention(it.intention)
                    }
                }
        }
    }

    fun dispatchIntention(intention: Any) {
        scope.launch {
            if (intention is GlobalIntention) {
                store.dispatch(intention)
            } else {
                intentionDispatcher.dispatchIntention(intention)
            }
        }
    }

    override fun close() {
        intentionDispatcher.close()
        store.unregister(this)
    }
}
