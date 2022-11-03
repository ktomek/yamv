package com.ktomek.yamv.ui.counter.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.feature.FeatureFlow
import com.ktomek.yamv.state.Store
import com.ktomek.yamv.ui.counter.logic.AutoDecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.StopAutoDecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.outcome.AutoDecreaseCounterOutcome
import com.ktomek.yamv.ui.counter.logic.outcome.CounterOutcome
import com.ktomek.yamv.ui.counter.logic.outcome.DecreaseCounterOutcome
import com.ktomek.yamv.ui.counter.logic.state.CounterState
import hu.akarnokd.kotlin.flow.takeUntil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AutoFeature
class AutoDecreaseFeature
@Inject constructor() : FeatureFlow<CounterOutcome>() {

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override suspend fun invoke(intentions: Flow<Any>, store: Store): Flow<CounterOutcome> =
        intentions
            .filterIsInstance<AutoDecreaseCounterIntention>()
            .flatMapMerge {
                store
                    .observeStates(CounterState::class.java)
                    .first()

                autoDecreaseFlow()
                    .takeUntil(intentions.filter { it is StopAutoDecreaseCounterIntention })
                    .onStart { emit(AutoDecreaseCounterOutcome(true)) }
                    .onCompletion { emit(AutoDecreaseCounterOutcome(false)) }
            }

    private fun autoDecreaseFlow(): Flow<CounterOutcome> = flow {
        while (true) {
            delay(1.seconds.inWholeMilliseconds)
            emit(DecreaseCounterOutcome)
        }
    }
}
