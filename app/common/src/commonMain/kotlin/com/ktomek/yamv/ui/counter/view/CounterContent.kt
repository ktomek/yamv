package com.ktomek.yamv.ui.counter.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun CounterContent(
    count: Int,
    autoIncreaseOn: Boolean,
    autoDecreaseOn: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAutoIncrease: () -> Unit,
    onAutoDecrease: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "$count")
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onDecrease) { Text(text = "Decrease") }
            Button(onClick = onIncrease) { Text(text = "Increase") }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onAutoDecrease) {
                Text(text = if (autoDecreaseOn) "Stop Auto Decrease" else "Auto Decrease")
            }
            Button(onClick = onAutoIncrease) {
                Text(text = if (autoIncreaseOn) "Stop Auto Increase" else "Auto Increase")
            }
        }
    }
}
