package com.ktomek.yamv.hilt

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Compose local carrying the app-root [MviStoreOwner].
 *
 * Defaults to the host [ComponentActivity] resolved from [LocalContext], so Hilt apps
 * get app-lifetime scoping without any setup — Hilt's `ViewModelProvider.Factory` already
 * treats the activity as the natural app-root owner. Override with [ProvideAppMviStoreOwner]
 * if you need to scope to a non-default owner (e.g. a specific Fragment or NavBackStackEntry).
 *
 * Mirrors `com.ktomek.yamv.koin.LocalAppMviStoreOwner` — but unlike the Koin variant this
 * one returns a useful default instead of erroring when no [ProvideAppMviStoreOwner] is set.
 */
val LocalAppMviStoreOwner = compositionLocalOf<MviStoreOwner?> { null }

/**
 * Override the app-root [MviStoreOwner] for the wrapped [content].
 *
 * Most Hilt apps don't need this — [hiltMviAppStore] and [LocalAppMviStoreOwner] already
 * resolve to the host `ComponentActivity` automatically. Use this only when the activity
 * isn't the right scope, e.g. for tests or when sharing across a Fragment-only sub-tree.
 *
 * The supplied [owner] must be a Hilt-aware `ViewModelStoreOwner` (Activity, Fragment,
 * NavBackStackEntry); Hilt's factory will fail at retrieval otherwise.
 */
@Composable
fun ProvideAppMviStoreOwner(
    owner: MviStoreOwner,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppMviStoreOwner provides owner, content = content)
}

/**
 * Convenience overload — captures the host [ComponentActivity] automatically and exposes it
 * as [LocalAppMviStoreOwner]. Mirrors Koin's `ProvideAppMviStoreOwner { … }` shape.
 *
 * Calling this is rarely necessary because [LocalAppMviStoreOwner] already defaults to the
 * activity; but it's available for parity with `yamv-koin`.
 */
@Composable
fun ProvideAppMviStoreOwner(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    ProvideAppMviStoreOwner(owner = activity, content = content)
}
