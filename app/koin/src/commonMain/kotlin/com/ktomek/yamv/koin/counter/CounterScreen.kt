package com.ktomek.yamv.koin.counter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.ktomek.yamv.koin.KoinMviStore
import com.ktomek.yamv.koin.counter.logic.state.CounterState
import com.ktomek.yamv.koin.koinMviStore
import com.ktomek.yamv.ui.counter.logic.AutoDecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.AutoIncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.DecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.IncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.StopAutoDecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.StopAutoIncreaseCounterIntention
import com.ktomek.yamv.ui.counter.view.CounterContent

@Composable
fun CounterScreen(
    store: KoinMviStore<CounterState> = koinMviStore()
) {
    val state by store.state.collectAsState()
    CounterContent(
        count = state.count,
        autoIncreaseOn = state.autoIncreaseOn,
        autoDecreaseOn = state.autoDecreaseOn,
        onIncrease = { store.dispatch(IncreaseCounterIntention) },
        onDecrease = { store.dispatch(DecreaseCounterIntention) },
        onAutoIncrease = {
            store.dispatch(
                if (state.autoIncreaseOn) StopAutoIncreaseCounterIntention
                else AutoIncreaseCounterIntention
            )
        },
        onAutoDecrease = {
            store.dispatch(
                if (state.autoDecreaseOn) StopAutoDecreaseCounterIntention
                else AutoDecreaseCounterIntention
            )
        },
    )
}
