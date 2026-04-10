package com.ktomek.yamv.viewmodel

import androidx.lifecycle.ViewModel
import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.state.MviStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstract lifecycle-retained MviStore host.
 *
 * Subclasses must override [store] with an [MviStore] implementation (typically [com.ktomek.yamv.state.MviRuntime]).
 * The [store] lifecycle is automatically tied to the retained lifecycle scope: [onCleared] calls [store].clear().
 *
 * @param S The state type.
 * @param I The intention type.
 */
abstract class MviRetainedStore<S : State, I : Any> : ViewModel(), MviStore<S, I> {

    protected abstract val store: MviStore<S, I>

    override val state: StateFlow<S>
        get() = store.state

    override val effects: Flow<EffectOutcome<S>>
        get() = store.effects

    override fun dispatch(intention: I) = store.dispatch(intention)

    override fun clear() = store.clear()

    override fun onCleared() = store.clear()
}
