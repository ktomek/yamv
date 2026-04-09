package com.ktomek.yamv.koin.counter.logic.feature

import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.functionTypedFeature
import com.ktomek.yamv.koin.counter.logic.state.CounterState
import com.ktomek.yamv.ui.counter.logic.IncreaseCounterIntention

val increaseFeature = functionTypedFeature<CounterState, IncreaseCounterIntention> {
    StateOutcome { it.copy(count = it.count + 1) }
}
