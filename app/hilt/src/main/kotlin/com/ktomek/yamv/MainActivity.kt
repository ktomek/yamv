package com.ktomek.yamv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ktomek.yamv.counter.CounterScreen
import com.ktomek.yamv.profile.ProfileScreen
import com.ktomek.yamv.ui.theme.YamvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YamvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    DemoHost()
                }
            }
        }
    }
}

private enum class Demo { COUNTER, PROFILE }

@Composable
private fun DemoHost() {
    var demo by rememberSaveable { mutableStateOf(Demo.COUNTER) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = { demo = Demo.COUNTER }, modifier = Modifier.weight(1f)) {
                Text("Counter")
            }
            Button(onClick = { demo = Demo.PROFILE }, modifier = Modifier.weight(1f)) {
                Text("Profile (UiState)")
            }
        }
        when (demo) {
            Demo.COUNTER -> CounterScreen()
            Demo.PROFILE -> ProfileScreen()
        }
    }
}
