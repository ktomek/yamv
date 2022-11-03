package com.ktomek.yamv.core

interface Outcome<S : State>

interface StateOutcome<S : State> : Outcome<S>

interface EffectOutcome<S : State> : Outcome<S>

interface IntentionOutcome<S : State> : Outcome<S> {
    val intention: Any
}
