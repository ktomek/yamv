package com.ktomek.yamv.state

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface representing a state store that can dispatch intentions and observe states and effects.
 *
 * @param S The type of the state.
 * @param E The type of the effect outcome.
 */
interface StateStore<S : State, I : Any> {
    /**
     * The current state as a [kotlinx.coroutines.flow.StateFlow].
     */
    val state: StateFlow<S>

    /**
     * The effects as a [kotlinx.coroutines.flow.Flow].
     */
    val effects: Flow<EffectOutcome<S>>

    /**
     * Dispatches an intention.
     *
     * @param intention The intention to be dispatched.
     */
    fun dispatch(intention: I)
}