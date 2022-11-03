package com.ktomek.yamv.ui.counter.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.feature.typedFeature
import com.ktomek.yamv.ui.counter.logic.IncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.outcome.ChangeCounterOutcome
import com.ktomek.yamv.ui.counter.logic.outcome.CounterOutcome

@AutoFeature
val increaseFeature = typedFeature<CounterOutcome, IncreaseCounterIntention> { _, _ ->
    ChangeCounterOutcome(1)
}
