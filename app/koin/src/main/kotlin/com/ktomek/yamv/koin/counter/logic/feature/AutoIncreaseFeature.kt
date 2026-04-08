package com.ktomek.yamv.koin.counter.logic.feature

import com.ktomek.yamv.feature.Feature.FlowFeature
import com.ktomek.yamv.koin.counter.logic.outcome.AutoIncreaseOutcome
import com.ktomek.yamv.koin.counter.logic.outcome.ChangeCounterOutcome
import com.ktomek.yamv.koin.counter.logic.outcome.CounterOutcome
import com.ktomek.yamv.koin.counter.logic.state.CounterState
import com.ktomek.yamv.ui.counter.logic.AutoIncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.StopAutoIncreaseCounterIntention
import hu.akarnokd.kotlin.flow.takeUntil
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class AutoIncreaseFeature constructor(private val f: DecreaseFeature) : FlowFeature<CounterState> {

    @OptIn(FlowPreview::class)
    override fun invoke(intentions: Flow<Any>): Flow<CounterOutcome> =
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
            val delay = Random.nextLong(5.seconds.inWholeMilliseconds)
            Timber.d("autoIncreaseFlow will wait $delay on ${Thread.currentThread().name}")
            delay(delay)
            Timber.d("autoIncreaseFlow waited $delay on ${Thread.currentThread().name}")
            emit(ChangeCounterOutcome(1))
        }
    }
}
