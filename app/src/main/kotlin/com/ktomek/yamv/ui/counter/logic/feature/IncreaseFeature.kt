package com.ktomek.yamv.ui.counter.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.feature.typedFeature
import com.ktomek.yamv.reducer.OutcomeWithReducer
import com.ktomek.yamv.ui.counter.logic.IncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.outcome.CounterOutcome
import com.ktomek.yamv.ui.counter.logic.outcome.CounterOutcomeWithReducer
import com.ktomek.yamv.ui.counter.logic.state.CounterState

@AutoFeature
val increaseFeature = typedFeature<CounterOutcome, IncreaseCounterIntention> { _, _ ->
    CounterOutcomeWithReducer { it.copy(count = it.count + 1) }
}

@AutoFeature
val increaseFeatureV2 = typedFeature<Outcome<CounterState>, IncreaseCounterIntention> { _, _ ->
    OutcomeWithReducer { it.copy(count = it.count + 1) }
}
