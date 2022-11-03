package com.ktomek.yamv.core

fun interface Reducer<S : State, R : Outcome<S>> {
    fun reduce(prevState: S, outcome: R): S
}
