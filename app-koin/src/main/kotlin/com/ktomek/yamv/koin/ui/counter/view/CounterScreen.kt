package com.ktomek.yamv.koin.ui.counter.view

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
import com.ktomek.yamv.koin.ui.counter.CounterViewModel
import com.ktomek.yamv.koin.ui.counter.logic.AutoDecreaseCounterIntention
import com.ktomek.yamv.koin.ui.counter.logic.AutoIncreaseCounterIntention
import com.ktomek.yamv.koin.ui.counter.logic.DecreaseCounterIntention
import com.ktomek.yamv.koin.ui.counter.logic.IncreaseCounterIntention
import com.ktomek.yamv.koin.ui.counter.logic.StopAutoDecreaseCounterIntention
import com.ktomek.yamv.koin.ui.counter.logic.StopAutoIncreaseCounterIntention
import com.ktomek.yamv.koin.ui.counter.logic.state.CounterState
import org.koin.androidx.compose.koinViewModel

@Composable
fun CounterScreen(
    viewModel: CounterViewModel = koinViewModel()
) {
    val state: CounterState by viewModel.state.collectAsState()

    Counter(
        counter = state.count,
        decrease = { viewModel.dispatch(DecreaseCounterIntention) },
        increase = {
            repeat(1) {
                viewModel.dispatch(IncreaseCounterIntention)
            }
        },
        autodecrease = {
            if (state.autoDecreaseOn) {
                viewModel.dispatch(StopAutoDecreaseCounterIntention)
            } else {
                viewModel.dispatch(AutoDecreaseCounterIntention)
            }
        },
        autoincrease = {
            if (state.autoIncreaseOn) {
                viewModel.dispatch(StopAutoIncreaseCounterIntention)
            } else {
                viewModel.dispatch(AutoIncreaseCounterIntention)
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
