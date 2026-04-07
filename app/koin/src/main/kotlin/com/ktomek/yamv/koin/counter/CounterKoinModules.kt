package com.ktomek.yamv.koin.counter

import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.wrap
import com.ktomek.yamv.ui.counter.logic.feature.AutoDecreaseFeature
import com.ktomek.yamv.ui.counter.logic.feature.AutoIncreaseFeature
import com.ktomek.yamv.ui.counter.logic.feature.DecreaseFeature
import com.ktomek.yamv.ui.counter.logic.feature.increaseFeature
import com.ktomek.yamv.ui.counter.logic.state.CounterState
import org.koin.dsl.module

val counterStateFeaturesKoinModule = module {
    factory<Feature<CounterState>> { AutoIncreaseFeature() }
    factory<Feature<CounterState>> { AutoDecreaseFeature() }
    factory<Feature<CounterState>> { DecreaseFeature().wrap() }
    factory<Feature<CounterState>> { increaseFeature }
}

val counterStateKoinModule = module {
    factory { CounterStateStore(getAll<Feature<CounterState>>().toSet()) }
}
