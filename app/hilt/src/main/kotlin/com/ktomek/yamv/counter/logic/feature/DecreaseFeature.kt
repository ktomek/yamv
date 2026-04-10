package com.ktomek.yamv.counter.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.counter.logic.outcome.CounterOutcome
import com.ktomek.yamv.counter.logic.outcome.DecreaseCounterOutcome
import com.ktomek.yamv.counter.logic.state.CounterState
import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.ui.counter.logic.DecreaseCounterIntention
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AutoFeature
class DecreaseFeature @Inject constructor() :
    FunctionTypedFeature<CounterState, DecreaseCounterIntention> {
    override suspend fun invoke(intention: DecreaseCounterIntention): CounterOutcome =
        withContext(Dispatchers.Default) { DecreaseCounterOutcome }
}
