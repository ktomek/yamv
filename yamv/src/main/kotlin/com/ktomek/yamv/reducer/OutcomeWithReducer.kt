package com.ktomek.yamv.reducer

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State

interface OutcomeWithReducer<S : State> : Outcome<S> {
    fun reduce(prevState: S): S
}
