package com.ktomek.yamv.ui.counter.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ktomek.yamv.core.ui.hiltStateStore
import com.ktomek.yamv.ui.counter.logic.AutoDecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.AutoIncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.DecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.IncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.StopAutoDecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.StopAutoIncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.state.CounterState
import com.ktomek.yamv.ui.counter.logic.state.CounterStateStore

@Composable
fun CounterScreen(
    store: CounterStateStore = hiltStateStore()
) {
    val state: CounterState by store.state.collectAsState(initial = CounterState(0))

    Counter(
        counter = state.count,
        decrease = { store.dispatch(DecreaseCounterIntention) },
        increase = {
            repeat(1) {
                store.dispatch(IncreaseCounterIntention)
            }
        },
        autodecrease = {
            if (state.autoDecreaseOn) {
                store.dispatch(StopAutoDecreaseCounterIntention)
            } else {
                store.dispatch(AutoDecreaseCounterIntention)
            }
        },
        autoincrease = {
            if (state.autoIncreaseOn) {
                store.dispatch(StopAutoIncreaseCounterIntention)
            } else {
                store.dispatch(AutoIncreaseCounterIntention)
            }
        },
        isAutoIncreaseOn = state.autoIncreaseOn,
        isAutoDecreaseOn = state.autoDecreaseOn
    )
}

@Composable
private fun Counter(
    counter: Int,
    decrease: () -> Unit,
    increase: () -> Unit,
    autodecrease: () -> Unit = {},
    autoincrease: () -> Unit = {},
    isAutoIncreaseOn: Boolean = false,
    isAutoDecreaseOn: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "$counter")
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { decrease() }) {
                Text(text = "Decrease")
            }
            Button(onClick = { increase() }) {
                Text(text = "Increase")
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { autodecrease() }) {
                if (isAutoDecreaseOn) {
                    Text(text = "Stop Auto Decrease")
                } else {
                    Text(text = "Auto Decrease")
                }
            }
            Button(onClick = { autoincrease() }) {
                if (isAutoIncreaseOn) {
                    Text(text = "Stop Auto Increase")
                } else {
                    Text(text = "Auto Increase")
                }
            }
        }
    }
}
