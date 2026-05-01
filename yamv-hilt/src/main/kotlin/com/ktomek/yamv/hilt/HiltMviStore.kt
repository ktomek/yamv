package com.ktomek.yamv.hilt

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ktomek.yamv.retainer.MviRetainedStore

/**
 * Returns a Hilt-provided [MviRetainedStore] instance scoped to [owner].
 *
 * Mirrors the underlying [hiltViewModel] signature so call sites can pass an explicit
 * [MviStoreOwner] (e.g. an Activity for app-lifetime scoping) or a [key] for keyed
 * instances. Defaults to [LocalMviStoreOwner] — the current navigation back-stack entry
 * when used inside a `NavHost`.
 *
 * Hilt's `ViewModelProvider.Factory` only resolves through Hilt-aware owners
 * ([ComponentActivity], `Fragment`, `NavBackStackEntry`); passing an arbitrary owner will
 * fail at retrieval time. Use [hiltMviAppStore] for the common app-lifetime case.
 *
 * Usage:
 * ```
 * @Composable
 * fun CounterScreen(store: CounterStateStore = hiltMviStore()) { ... }
 * ```
 */
@Composable
inline fun <reified VM : MviRetainedStore<*, *>> hiltMviStore(
    owner: MviStoreOwner = checkNotNull(LocalMviStoreOwner.current) {
        "No MviStoreOwner in composition (LocalMviStoreOwner.current was null)"
    },
    key: String? = null,
): VM = hiltViewModel(viewModelStoreOwner = owner, key = key)

/**
 * Returns a Hilt-provided [MviRetainedStore] scoped to the app-root [MviStoreOwner], so the
 * same instance survives navigation changes and is shared across screens.
 *
 * Resolves [LocalAppMviStoreOwner.current] when set (via [ProvideAppMviStoreOwner]), or
 * falls back to the host [ComponentActivity] discovered from [LocalContext]. The activity
 * is the canonical app-lifetime owner for Hilt-managed ViewModels, so most apps need no
 * extra setup.
 *
 * Mirrors `com.ktomek.yamv.koin.koinMviAppStore` from `yamv-koin`.
 *
 * Usage:
 * ```
 * @Composable
 * fun AuthScreen(store: AuthStateStore = hiltMviAppStore()) { ... }
 * ```
 *
 * @throws IllegalStateException when no [LocalAppMviStoreOwner] is set and the host context
 *   cannot be resolved to a [ComponentActivity] — required because Hilt's ViewModel factory
 *   only works with Hilt-aware owners.
 */
@Composable
inline fun <reified VM : MviRetainedStore<*, *>> hiltMviAppStore(key: String? = null): VM {
    val explicit = LocalAppMviStoreOwner.current
    val owner = if (explicit != null) {
        explicit
    } else {
        val context = LocalContext.current
        remember(context) { context.findComponentActivity() }
    }
    return hiltMviStore(owner = owner, key = key)
}

@PublishedApi
internal fun Context.findComponentActivity(): ComponentActivity {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    error(
        "hiltMviAppStore requires the host context to resolve to a ComponentActivity " +
            "(got ${this::class.qualifiedName}). Place the composable inside an activity that " +
            "extends ComponentActivity, or wrap the subtree with ProvideAppMviStoreOwner(owner = ...).",
    )
}
