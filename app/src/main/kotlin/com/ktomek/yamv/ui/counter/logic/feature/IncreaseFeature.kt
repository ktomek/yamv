package com.ktomek.yamv.ui.counter.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.typedFeature
import com.ktomek.yamv.ui.counter.logic.IncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.state.CounterState

@AutoFeature
val increaseFeature = typedFeature<CounterState, IncreaseCounterIntention> { _, _ ->
    StateOutcome { it.copy(count = it.count + 1) }
}

@AutoFeature
val increaseFeatureV2 = typedFeature<CounterState, IncreaseCounterIntention> { _, _ ->
    StateOutcome { it.copy(count = it.count + 1) }
}
