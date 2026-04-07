package com.ktomek.yamv.koin.ui.counter.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.koin.ui.counter.logic.DecreaseCounterIntention
import com.ktomek.yamv.koin.ui.counter.logic.outcome.CounterOutcome
import com.ktomek.yamv.koin.ui.counter.logic.outcome.DecreaseCounterOutcome
import com.ktomek.yamv.koin.ui.counter.logic.state.CounterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AutoFeature
class DecreaseFeature : FunctionTypedFeature<CounterState, DecreaseCounterIntention> {
    override suspend fun invoke(intention: DecreaseCounterIntention): CounterOutcome =
        withContext(Dispatchers.Default) { DecreaseCounterOutcome }
}
