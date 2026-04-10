package com.ktomek.yamv.hilt

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ktomek.yamv.retainer.MviRetainedStore

/**
 * Returns a Hilt-provided [MviRetainedStore] instance scoped to the current backstack entry.
 *
 * Prefer this over calling [hiltViewModel] directly so call sites stay decoupled from
 * the Hilt navigation API and the type constraint on [MviRetainedStore] is enforced.
 *
 * Usage:
 * ```
 * @Composable
 * fun CounterScreen(store: CounterStateStore = hiltMviStore()) { ... }
 * ```
 */
@Composable
inline fun <reified VM : MviRetainedStore<*, *>> hiltMviStore(): VM = hiltViewModel()
