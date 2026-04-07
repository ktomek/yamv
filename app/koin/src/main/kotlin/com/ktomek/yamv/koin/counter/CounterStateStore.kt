package com.ktomek.yamv.koin.counter

import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.state.MviRuntime
import com.ktomek.yamv.state.MviStore
import com.ktomek.yamv.ui.counter.logic.state.CounterState

class CounterStateStore(features: Set<Feature<CounterState>>) {
    val store: MviStore<CounterState, Any> = MviRuntime(
        features = features,
        defaultState = CounterState(),
    )
}
