package com.ktomek.yamv.hilt

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.ktomek.yamv.viewmodel.MviViewModel

/**
 * Returns a Hilt-provided [MviViewModel] instance scoped to the current backstack entry.
 *
 * Prefer this over calling [hiltViewModel] directly so call sites stay decoupled from
 * the Hilt navigation API and the type constraint on [MviViewModel] is enforced.
 *
 * Usage:
 * ```
 * @Composable
 * fun CounterScreen(store: CounterStateStore = hiltMviStore()) { ... }
 * ```
 */
@Composable
inline fun <reified VM : MviViewModel<*, *>> hiltMviStore(): VM = hiltViewModel()
