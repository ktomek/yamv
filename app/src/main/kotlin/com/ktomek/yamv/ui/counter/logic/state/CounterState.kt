package com.ktomek.yamv.ui.counter.logic.state

import com.ktomek.yamv.annotations.AutoState
import com.ktomek.yamv.core.State

@AutoState
data class CounterState(
    val count: Int = 0,
    val autoIncreaseOn: Boolean = false,
    val autoDecreaseOn: Boolean = false
) : State
