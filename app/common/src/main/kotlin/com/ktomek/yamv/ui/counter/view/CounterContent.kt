package com.ktomek.yamv.ui.counter.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ktomek.yamv.ui.counter.logic.AutoDecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.AutoIncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.DecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.IncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.StopAutoDecreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.StopAutoIncreaseCounterIntention
import com.ktomek.yamv.ui.counter.logic.state.CounterState

@Composable
fun CounterContent(
    state: CounterState,
    onDispatch: (Any) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "${state.count}")
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onDispatch(DecreaseCounterIntention) }) {
                Text(text = "Decrease")
            }
            Button(onClick = { onDispatch(IncreaseCounterIntention) }) {
                Text(text = "Increase")
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                onDispatch(
                    if (state.autoDecreaseOn) StopAutoDecreaseCounterIntention
                    else AutoDecreaseCounterIntention
                )
            }) {
                Text(text = if (state.autoDecreaseOn) "Stop Auto Decrease" else "Auto Decrease")
            }
            Button(onClick = {
                onDispatch(
                    if (state.autoIncreaseOn) StopAutoIncreaseCounterIntention
                    else AutoIncreaseCounterIntention
                )
            }) {
                Text(text = if (state.autoIncreaseOn) "Stop Auto Increase" else "Auto Increase")
            }
        }
    }
}
