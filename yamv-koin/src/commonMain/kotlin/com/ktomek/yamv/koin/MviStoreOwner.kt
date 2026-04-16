package com.ktomek.yamv.koin

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * The lifecycle-scoped owner that retains an MVI store instance.
 *
 * Typealiased to [ViewModelStoreOwner] — the underlying mechanism is the multiplatform
 * ViewModel store, but public APIs prefer this name so the MVI surface stays free of
 * ViewModel terminology.
 */
typealias MviStoreOwner = ViewModelStoreOwner

/**
 * Access the current [MviStoreOwner] from composition.
 *
 * Delegates to [LocalViewModelStoreOwner]. In Compose Multiplatform's Navigation this is
 * overridden per `NavBackStackEntry`, which is why app-lifetime state requires capturing
 * the root owner explicitly — see [ProvideAppMviStoreOwner].
 */
@Suppress("ClassName", "ClassNaming")
object LocalMviStoreOwner {
    val current: MviStoreOwner?
        @Composable
        get() = LocalViewModelStoreOwner.current
}
