package com.ktomek.yamv.reducer

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State

/**
 * Interface representing an outcome that includes a reducer function to produce a new state.
 *
 * @param S The type of the state.
 */
interface OutcomeWithReducer<S : State> : Outcome<S> {

    /**
     * Reduces the previous state to produce a new state.
     *
     * @param prevState The previous state.
     * @return The new state produced by applying the outcome to the previous state.
     */
    fun reduce(prevState: S): S
}
