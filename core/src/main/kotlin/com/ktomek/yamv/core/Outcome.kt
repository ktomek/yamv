package com.ktomek.yamv.core

/**
 * Interface representing an outcome that produces a state.
 *
 * @param S The type of the state.
 */
interface Outcome<S : State>

/**
 * Interface representing an outcome that produces a state and can be used as a state outcome.
 *
 * @param S The type of the state.
 */
interface StateOutcome<S : State> : Outcome<S>

/**
 * Interface representing an outcome that produces a state and can be used as an effect outcome.
 *
 * @param S The type of the state.
 */
interface EffectOutcome<S : State> : Outcome<S>

/**
 * Interface representing an outcome that produces a state and includes an intention.
 *
 * @param S The type of the state.
 */
interface IntentionOutcome<S : State> : Outcome<S> {
    /**
     * The intention associated with this outcome.
     */
    val intention: Any
}