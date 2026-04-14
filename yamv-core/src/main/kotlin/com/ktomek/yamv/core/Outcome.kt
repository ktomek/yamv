package com.ktomek.yamv.core

/**
 * Interface representing an outcome that produces a state.
 *
 * @param S The type of the state.
 */
interface Outcome<out S : State>

/**
 * SAM type representing an outcome that also reduces state.
 * Allows lambda usage: `val o: StateOutcome<S> = { prev -> prev }`
 */
fun interface StateOutcome<S : State> : Outcome<S>, Reducer<S>

/**
 * Marker for side-effect outcomes.
 */
interface EffectOutcome<out S : State> : Outcome<S>

/**
 * Outcome that requests dispatching another intention.
 */
interface IntentionOutcome<out S : State> : Outcome<S> {
    val intention: Any
}

fun interface Reducer<S : State> {
    fun reduce(prevState: S): S
}
