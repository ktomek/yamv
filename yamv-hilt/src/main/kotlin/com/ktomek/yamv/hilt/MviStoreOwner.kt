package com.ktomek.yamv.hilt

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * The lifecycle-scoped owner that retains an MVI store instance.
 *
 * Typealiased to [ViewModelStoreOwner] — the underlying mechanism is the Android ViewModel
 * store, but public APIs prefer this name so the MVI surface stays free of ViewModel
 * terminology. Mirrors `com.ktomek.yamv.koin.MviStoreOwner` from `yamv-koin`.
 */
typealias MviStoreOwner = ViewModelStoreOwner

/**
 * Access the current [MviStoreOwner] from composition.
 *
 * Delegates to [LocalViewModelStoreOwner]. Inside a `NavHost` this is overridden per
 * `NavBackStackEntry`, so each navigation destination gets its own store instance — see
 * [hiltMviAppStore] / [LocalAppMviStoreOwner] for state that should survive navigation.
 */
@Suppress("ClassName", "ClassNaming")
object LocalMviStoreOwner {
    val current: MviStoreOwner?
        @Composable
        get() = LocalViewModelStoreOwner.current
}
