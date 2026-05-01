package com.ktomek.yamv.state

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MviStore<S : State, I : Any> {
    val state: StateFlow<S>
    val effects: Flow<EffectOutcome<S>>
    fun dispatch(intention: I)
    fun clear()

    /** Sugar — `store(intention)` is equivalent to `store.dispatch(intention)`. */
    operator fun invoke(intention: I) = dispatch(intention)

    /** Sugar — `store send intention` is equivalent to `store.dispatch(intention)`. */
    infix fun send(intention: I) = dispatch(intention)
}
