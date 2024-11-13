package com.ktomek.yamv.core

/**
 * Interface representing an outcome that produces a state.
 *
 * @param S The type of the state.
 */
interface Outcome<out S : State>

/**
 * Interface representing an outcome that produces a state and can be used as a state outcome.
 *
 * @param S The type of the state.
 */
fun interface StateOutcome<S : State> : Outcome<S> {
    /**
     * Reduces the previous state to produce a new state.
     *
     * @param prevState The previous state.
     * @return The new state produced by applying the outcome to the previous state.
     */
    fun reduce(prevState: S): S
}

/**
 * Interface representing an outcome that produces a state and can be used as an effect outcome.
 *
 * @param S The type of the state.
 */
interface EffectOutcome<out S : State> : Outcome<S>

/**
 * Interface representing an outcome that produces a state and includes an intention.
 *
 * @param S The type of the state.
 */
interface IntentionOutcome<out S : State> : Outcome<S> {
    /**
     * The intention associated with this outcome.
     */
    val intention: Any
}
