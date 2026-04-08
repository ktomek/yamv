package com.ktomek.yamv.ui.counter.logic.outcome

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.ui.counter.logic.state.CounterState

typealias CounterOutcome = Outcome<CounterState>
typealias CounterOutcomeWithReducer = StateOutcome<CounterState>

data class ChangeCounterOutcome(val value: Int) : StateOutcome<CounterState> {
    override fun reduce(prevState: CounterState): CounterState =
        prevState.copy(count = prevState.count + value)
}

val DecreaseCounterOutcome = CounterOutcomeWithReducer {
    it.copy(count = it.count - 1)
}

data class AutoDecreaseCounterOutcome(val isOn: Boolean) : CounterOutcomeWithReducer {
    override fun reduce(prevState: CounterState): CounterState =
        prevState.copy(autoDecreaseOn = isOn)
}

data class AutoIncreaseOutcome(val isOn: Boolean) : CounterOutcomeWithReducer {
    override fun reduce(prevState: CounterState): CounterState =
        prevState.copy(autoIncreaseOn = isOn)
}
