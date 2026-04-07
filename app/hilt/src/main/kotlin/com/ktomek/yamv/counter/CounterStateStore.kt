package com.ktomek.yamv.counter

import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.state.MviRuntime
import com.ktomek.yamv.ui.counter.logic.state.CounterState
import com.ktomek.yamv.viewmodel.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CounterStateStore @Inject constructor(
    private val features: @JvmSuppressWildcards Set<Feature<CounterState>>,
) : MviViewModel<CounterState, Any>() {
    override val store = MviRuntime(
        features = features,
        defaultState = CounterState(),
    )
}
