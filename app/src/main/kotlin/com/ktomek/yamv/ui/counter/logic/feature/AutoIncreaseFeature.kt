package com.ktomek.yamv.ui.counter.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.feature.FeatureFlow
import com.ktomek.yamv.state.Store
import com.ktomek.yamv.ui.counter.logic.AutoIncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.StopAutoIncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.outcome.AutoIncreaseOutcome
import com.ktomek.yamv.ui.counter.logic.outcome.ChangeCounterOutcome
import com.ktomek.yamv.ui.counter.logic.outcome.CounterOutcome
import com.ktomek.yamv.ui.counter.logic.state.CounterState
import hu.akarnokd.kotlin.flow.takeUntil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@AutoFeature
class AutoIncreaseFeature
@Inject constructor() : FeatureFlow<CounterState>() {
    override val dispatcher: CoroutineDispatcher
        get() = Dispatchers.Default

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override suspend fun invoke(intentions: Flow<Any>, store: Store): Flow<CounterOutcome> =
        intentions
            .filter { it is AutoIncreaseCounterIntention }
            .flatMapMerge {
                autoIncreaseFlow()
                    .takeUntil(intentions.filter { it is StopAutoIncreaseCounterIntention })
                    .onStart { emit(AutoIncreaseOutcome(true)) }
                    .onCompletion { emit(AutoIncreaseOutcome(false)) }
            }

    private fun autoIncreaseFlow() = flow<CounterOutcome> {
        while (true) {
//            delay(1000)
            val delay = Random.nextLong(5.seconds.inWholeMilliseconds)
            Timber.d("autoIncreaseFlow will wait $delay on ${Thread.currentThread().name}")
            delay(delay)
            Timber.d("autoIncreaseFlow waited $delay on ${Thread.currentThread().name}")
            emit(ChangeCounterOutcome(1))
        }
    }
}
