package com.ktomek.yamv.ui.counter.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.feature.TypedFeature
import com.ktomek.yamv.state.Store
import com.ktomek.yamv.ui.counter.logic.DecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.outcome.CounterOutcome
import com.ktomek.yamv.ui.counter.logic.outcome.DecreaseCounterOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AutoFeature
class DecreaseFeature @Inject constructor() :
    TypedFeature<CounterOutcome, DecreaseCounterIntention>() {
    override suspend fun invoke(intention: DecreaseCounterIntention, store: Store): CounterOutcome =
        withContext(Dispatchers.Default) { DecreaseCounterOutcome }
}
