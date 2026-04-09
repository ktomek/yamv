package com.ktomek.yamv.koin.counter

import com.ktomek.yamv.feature.wrap
import com.ktomek.yamv.koin.counter.logic.feature.AutoDecreaseFeature
import com.ktomek.yamv.koin.counter.logic.feature.AutoIncreaseFeature
import com.ktomek.yamv.koin.counter.logic.feature.DecreaseFeature
import com.ktomek.yamv.koin.counter.logic.feature.increaseFeature
import com.ktomek.yamv.koin.counter.logic.state.CounterState
import com.ktomek.yamv.koin.mviStore
import org.koin.dsl.module

val counterModule = module {
    factory { AutoDecreaseFeature() }
    factory { DecreaseFeature() }
    factory { AutoIncreaseFeature(get()) }

    mviStore(defaultState = CounterState()) {
        setOf(
            get<AutoDecreaseFeature>(),
            get<AutoIncreaseFeature>(),
            get<DecreaseFeature>().wrap(),
            increaseFeature,
        )
    }
}
