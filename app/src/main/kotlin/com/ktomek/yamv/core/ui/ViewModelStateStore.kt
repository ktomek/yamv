package com.ktomek.yamv.core.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.ktomek.yamv.state.ViewModelStateStore

@Composable
inline fun <reified C : ViewModelStateStore<*, *, *>> hiltStateStore(): C = hiltViewModel()
