package com.ktomek.yamv.koin.counter.logic.state

import com.ktomek.yamv.core.State

data class CounterState(
    val count: Int = 0,
    val autoIncreaseOn: Boolean = false,
    val autoDecreaseOn: Boolean = false
) : State
