package com.ktomek.yamv.core

/**
 * Functional interface representing a reducer that processes a previous state and an outcome to produce a new state.
 *
 * @param S The type of the state.
 * @param O The type of the outcome that produces the new state.
 */
fun interface Reducer<S : State, O : Outcome<S>> {

    /**
     * Reduces the previous state and an outcome to produce a new state.
     *
     * @param prevState The previous state.
     * @param outcome The outcome that produces the new state.
     * @return The new state produced by applying the outcome to the previous state.
     */
    fun reduce(prevState: S, outcome: O): S
}
