package com.ktomek.yamv.koin.counter.logic.feature

import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.koin.counter.logic.outcome.CounterOutcome
import com.ktomek.yamv.koin.counter.logic.outcome.DecreaseCounterOutcome
import com.ktomek.yamv.koin.counter.logic.state.CounterState
import com.ktomek.yamv.ui.counter.logic.DecreaseCounterIntention
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DecreaseFeature : FunctionTypedFeature<CounterState, DecreaseCounterIntention> {
    override suspend fun invoke(intention: DecreaseCounterIntention): CounterOutcome =
        withContext(Dispatchers.Default) { DecreaseCounterOutcome }
}
