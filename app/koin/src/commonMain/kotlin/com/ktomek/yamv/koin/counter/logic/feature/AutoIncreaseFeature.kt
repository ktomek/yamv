package com.ktomek.yamv.koin.counter.logic.feature

import com.ktomek.yamv.feature.Feature.FlowFeature
import com.ktomek.yamv.koin.counter.logic.outcome.AutoIncreaseOutcome
import com.ktomek.yamv.koin.counter.logic.outcome.ChangeCounterOutcome
import com.ktomek.yamv.koin.counter.logic.outcome.CounterOutcome
import com.ktomek.yamv.koin.counter.logic.state.CounterState
import com.ktomek.yamv.ui.counter.logic.AutoIncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.StopAutoIncreaseCounterIntention
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class AutoIncreaseFeature constructor(private val f: DecreaseFeature) : FlowFeature<CounterState> {

    @OptIn(FlowPreview::class)
    override fun invoke(intentions: Flow<Any>): Flow<CounterOutcome> =
        intentions
            .filter { it is AutoIncreaseCounterIntention }
            .flatMapMerge {
                autoIncreaseFlow()
                    .takeUntilSignal(intentions.filter { it is StopAutoIncreaseCounterIntention })
                    .onStart { emit(AutoIncreaseOutcome(true)) }
                    .onCompletion { emit(AutoIncreaseOutcome(false)) }
            }

    private fun autoIncreaseFlow() = flow<CounterOutcome> {
        while (true) {
            val delayMs = Random.nextLong(5.seconds.inWholeMilliseconds)
            delay(delayMs)
            emit(ChangeCounterOutcome(1))
        }
    }
}
