package com.ktomek.yamv.koin

import androidx.compose.runtime.Composable
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.state.MviRuntime
import com.ktomek.yamv.state.MviStore
import com.ktomek.yamv.viewmodel.MviViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

/**
 * Generic ViewModel that drives any [State] type through the MVI runtime.
 *
 * Replaces per-state generated classes (e.g. `CounterStateStore`). Register it via [mviStore]
 * and retrieve it via [koinMviStore] — both derive the Koin qualifier from [S] automatically.
 */
class KoinMviStore<S : State>(
    features: Set<Feature<S>>,
    defaultState: S,
) : MviViewModel<S, Any>() {
    override val store: MviStore<S, Any> = MviRuntime(
        features = features,
        defaultState = defaultState,
    )
}

/**
 * Returns a [Qualifier] derived from the simple name of [S].
 *
 * Used internally by [mviStore] and [koinMviStore] to ensure the registration qualifier and
 * the resolution qualifier always match without any string literals in user code.
 */
inline fun <reified S : State> stateQualifier(): Qualifier = named(S::class.simpleName!!)

/**
 * Registers a [KoinMviStore] for state type [S] as a Koin ViewModel, qualified by [stateQualifier].
 *
 * Call this inside a `module { }` block. The [features] lambda runs in [Scope] so `get<T>()`
 * is available for retrieving feature instances registered elsewhere in the module.
 *
 * Usage:
 * ```kotlin
 * val counterModule = module {
 *     factory { AutoDecreaseFeature() }
 *
 *     mviStore(defaultState = CounterState()) {
 *         setOf(get<AutoDecreaseFeature>(), ...)
 *     }
 * }
 * ```
 */
inline fun <reified S : State> Module.mviStore(
    defaultState: S,
    noinline features: Scope.() -> Set<Feature<S>>,
) {
    viewModel(stateQualifier<S>()) {
        KoinMviStore(features = features(), defaultState = defaultState)
    }
}

/**
 * Returns the [KoinMviStore] for state type [S] scoped to the current navigation back-stack entry.
 *
 * The qualifier is derived from [S] automatically — no string literals needed at call sites.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun CounterScreen(store: KoinMviStore<CounterState> = koinMviStore()) { ... }
 * ```
 */
@Composable
inline fun <reified S : State> koinMviStore(): KoinMviStore<S> =
    koinViewModel(stateQualifier<S>())
