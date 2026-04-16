package com.ktomek.yamv.koin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.ktomek.yamv.core.State
import com.ktomek.yamv.state.MviStore

/**
 * Compose local carrying the app-root [MviStoreOwner], set by [ProvideAppMviStoreOwner].
 *
 * Use together with [koinMviAppStore] to obtain stores scoped to the entire app lifetime
 * (surviving navigation changes), not to the current back-stack entry.
 */
val LocalAppMviStoreOwner = staticCompositionLocalOf<MviStoreOwner> {
    error("ProvideAppMviStoreOwner { ... } not set. Wrap your NavHost (or root composable) with it.")
}

/**
 * Captures the current [MviStoreOwner] as the app-root owner and exposes it via
 * [LocalAppMviStoreOwner] to descendants.
 *
 * Place this **above** your `NavHost`, because `NavHost` overrides
 * `LocalViewModelStoreOwner` per back-stack entry.
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     ProvideAppMviStoreOwner {
 *         NavHost(...) { ... }
 *     }
 * }
 * ```
 */
@Composable
fun ProvideAppMviStoreOwner(content: @Composable () -> Unit) {
    val rootOwner = checkNotNull(LocalMviStoreOwner.current) {
        "No MviStoreOwner in composition"
    }
    CompositionLocalProvider(LocalAppMviStoreOwner provides rootOwner, content = content)
}

/**
 * Returns an [MviStore] scoped to the app-root owner captured by [ProvideAppMviStoreOwner].
 *
 * All calls from anywhere in the composition resolve to the same instance, regardless of
 * the current navigation entry.
 */
@Composable
inline fun <reified S : State, reified I : Any> koinMviAppStore(): MviStore<S, I> =
    koinMviStore(LocalAppMviStoreOwner.current)
