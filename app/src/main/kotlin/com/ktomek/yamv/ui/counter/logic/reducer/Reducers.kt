package com.ktomek.yamv.ui.counter.logic.reducer

import com.ktomek.yamv.annotations.AutoReducer
import com.ktomek.yamv.core.Reducer
import com.ktomek.yamv.ui.counter.logic.outcome.ChangeCounterOutcome
import com.ktomek.yamv.ui.counter.logic.outcome.Object2CounterResult
import com.ktomek.yamv.ui.counter.logic.outcome.ObjectCounterResult
import com.ktomek.yamv.ui.counter.logic.state.CounterState

@AutoReducer
val ChangeCounterReducer = Reducer<CounterState, ChangeCounterOutcome> { prevState, result ->
    prevState
        .copy(count = prevState.count + result.value)
}

@AutoReducer
object ObjectReducer : Reducer<CounterState, Object2CounterResult> {
    override fun reduce(prevState: CounterState, outcome: Object2CounterResult): CounterState =
        prevState.copy(count = -100)
}

@AutoReducer
class ObjectCounterReducer : Reducer<CounterState, ObjectCounterResult> {
    override fun reduce(prevState: CounterState, outcome: ObjectCounterResult): CounterState =
        prevState
}
