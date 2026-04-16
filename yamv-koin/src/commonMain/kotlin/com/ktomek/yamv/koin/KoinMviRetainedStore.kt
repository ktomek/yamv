package com.ktomek.yamv.koin

import androidx.compose.runtime.Composable
import com.ktomek.yamv.annotations.OpenForTesting
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.retainer.MviRetainedStore
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig
import com.ktomek.yamv.state.MviRuntime
import com.ktomek.yamv.state.MviStore
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

/**
 * Generic ViewModel that drives any [State] type through the MVI runtime.
 *
 * Replaces per-state generated classes (e.g. `CounterStateStore`). Register it via [mviStore]
 * and retrieve it via [koinMviStore] — both derive the Koin qualifier from [S] automatically.
 *
 * @param S state type
 * @param I intention type — narrows [MviStore.dispatch] at compile time. Use [Any] for untyped.
 */
@OpenForTesting
class KoinMviRetainedStore<S : State, I : Any>(
    features: Set<Feature<S>>,
    defaultState: S,
    dispatcherConfig: CoroutineDispatcherConfig = DefaultCoroutineDispatcherConfig(),
) : MviRetainedStore<S, I>() {
    override val dispatcherConfig: CoroutineDispatcherConfig = dispatcherConfig
    override val store: MviStore<S, I> =
        @Suppress("UNCHECKED_CAST")
        (
            MviRuntime(
                features = features,
                defaultState = defaultState,
                dispatcherConfig = dispatcherConfig,
            ) as MviStore<S, I>
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
 * Registers a [KoinMviRetainedStore] for state type [S] and intention type [I] as a Koin
 * ViewModel, qualified by [stateQualifier].
 *
 * Call this inside a `module { }` block. The [features] lambda receives a [FeatureRegistrar]
 * so `add(feature)` and `get<T>()` are available without calling `.wrap()` manually.
 *
 * Usage:
 * ```kotlin
 * val counterModule = module {
 *     factory { AutoDecreaseFeature() }
 *     factory { DecreaseFeature() }
 *
 *     mviStore<CounterState, CounterIntention>(defaultState = CounterState()) {
 *         add(get<AutoDecreaseFeature>())
 *         add(get<DecreaseFeature>())
 *     }
 * }
 * ```
 */
inline fun <reified S : State, reified I : Any> Module.mviStore(
    defaultState: S,
    dispatcherConfig: CoroutineDispatcherConfig = DefaultCoroutineDispatcherConfig(),
    crossinline features: FeatureRegistrar<S>.() -> Unit,
) {
    viewModel(stateQualifier<S>()) {
        val registrar = FeatureRegistrar<S>(this)
        registrar.features()
        KoinMviRetainedStore<S, I>(
            features = registrar.build(),
            defaultState = defaultState,
            dispatcherConfig = dispatcherConfig,
        )
    }
}

/**
 * Returns the [MviStore] for state type [S] and intention type [I], scoped to [storeOwner].
 *
 * By default [storeOwner] is `LocalViewModelStoreOwner.current`, which in Compose Multiplatform's
 * Navigation is overridden per `NavBackStackEntry` — so each navigation destination gets its own
 * store instance. For app-lifetime state (auth, session, preferences) pass an app-level owner,
 * or use [koinMviAppStore] together with [ProvideAppMviStoreOwner].
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun CounterScreen(
 *     store: MviStore<CounterState, CounterIntention> = koinMviStore(),
 * ) { ... }
 * ```
 */
@Composable
inline fun <reified S : State, reified I : Any> koinMviStore(
    storeOwner: MviStoreOwner = checkNotNull(LocalMviStoreOwner.current) {
        "No MviStoreOwner in composition (LocalMviStoreOwner.current was null)"
    },
): MviStore<S, I> =
    koinViewModel<KoinMviRetainedStore<S, I>>(
        qualifier = stateQualifier<S>(),
        viewModelStoreOwner = storeOwner,
    )
