package com.ktomek.yamv.counter.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.functionTypedFeature
import com.ktomek.yamv.ui.counter.logic.IncreaseCounterIntention
import com.ktomek.yamv.counter.logic.state.CounterState

@AutoFeature
val increaseFeature = functionTypedFeature<CounterState, IncreaseCounterIntention> {
    StateOutcome { it.copy(count = it.count + 1) }
}
