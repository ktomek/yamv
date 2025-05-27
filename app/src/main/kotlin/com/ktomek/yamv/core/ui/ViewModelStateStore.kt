package com.ktomek.yamv.core.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.ktomek.yamv.state.StateContainerHost

@Composable
inline fun <reified C : StateContainerHost<*>> hiltStateStore(): C = hiltViewModel()
