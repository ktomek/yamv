package com.ktomek.yamv.ui.counter.logic.outcome

import com.ktomek.yamv.annotations.AutoOutcome
import com.ktomek.yamv.ui.counter.logic.state.CounterState

@AutoOutcome
data class ChangeCounterOutcome(val value: Int) : CounterOutcome

@AutoOutcome
val DecreaseCounterOutcome = CounterOutcomeWithReducer {
    it.copy(count = it.count - 1)
}

@AutoOutcome
object ObjectCounterResult : CounterOutcome

@AutoOutcome
object Object2CounterResult : CounterOutcome

@AutoOutcome
object ObjectCounterOutcomeWithReducer : CounterOutcomeWithReducer {
    override fun reduce(prevState: CounterState): CounterState =
        prevState.copy(count = 100)
}

@AutoOutcome
data class AutoDecreaseCounterOutcome(val isOn: Boolean) : CounterOutcomeWithReducer {
    override fun reduce(prevState: CounterState): CounterState =
        prevState.copy(autoDecreaseOn = isOn)
}

@AutoOutcome
data class AutoIncreaseOutcome(val isOn: Boolean) : CounterOutcomeWithReducer {
    override fun reduce(prevState: CounterState): CounterState =
        prevState.copy(autoIncreaseOn = isOn)
}
