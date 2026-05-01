package com.ktomek.yamv.hilt

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.ktomek.yamv.retainer.MviRetainedStore

/**
 * Returns a Hilt-provided [MviRetainedStore] instance scoped to [owner].
 *
 * Mirrors the underlying [hiltViewModel] signature so call sites can pass an explicit
 * [ViewModelStoreOwner] (e.g. an Activity for app-lifetime scoping) or a [key] for keyed
 * instances. Defaults to `LocalViewModelStoreOwner.current` — the current navigation
 * back-stack entry when used inside a `NavHost`.
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
    owner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner in composition (LocalViewModelStoreOwner.current was null)"
    },
    key: String? = null,
): VM = hiltViewModel(viewModelStoreOwner = owner, key = key)

/**
 * Returns a Hilt-provided [MviRetainedStore] scoped to the host [ComponentActivity], so the
 * same instance survives navigation changes and is shared across screens for the lifetime
 * of the activity.
 *
 * Walks `LocalContext.current` through any [ContextWrapper] chain to find the activity, so
 * this works under `NavHost` (which overrides `LocalViewModelStoreOwner` per back-stack
 * entry) without any extra setup.
 *
 * Mirrors [com.ktomek.yamv.koin.koinMviAppStore] from `yamv-koin`.
 *
 * Usage:
 * ```
 * @Composable
 * fun AuthScreen(store: AuthStateStore = hiltMviAppStore()) { ... }
 * ```
 *
 * @throws IllegalStateException if the host context cannot be resolved to a [ComponentActivity]
 *   — required because Hilt's ViewModel factory only works with Hilt-aware owners.
 */
@Composable
inline fun <reified VM : MviRetainedStore<*, *>> hiltMviAppStore(key: String? = null): VM {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    return hiltMviStore(owner = activity, key = key)
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
            "extends ComponentActivity or one of its subclasses (e.g. AppCompatActivity).",
    )
}
