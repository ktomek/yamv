package com.ktomek.yamv.reducer

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.Reducer
import com.ktomek.yamv.core.State
import javax.inject.Inject

class DefaultReducer<S : State, O : Outcome<S>>
@Inject
constructor(
    private val reducers: Map<Class<out O>, @JvmSuppressWildcards Reducer<S, out O>>
) : Reducer<S, O> {
    @Suppress("UNCHECKED_CAST")
    override fun reduce(prevState: S, outcome: O): S =
        when (outcome) {
            is OutcomeWithReducer<*> -> (outcome as? OutcomeWithReducer<S>)?.reduce(prevState)
            else -> (reducers[outcome::class.java] as? Reducer<S, O>)?.reduce(prevState, outcome)
        } ?: prevState
}
