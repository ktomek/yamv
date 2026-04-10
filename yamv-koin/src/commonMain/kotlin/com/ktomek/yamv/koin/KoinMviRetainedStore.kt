package com.ktomek.yamv.koin

import androidx.compose.runtime.Composable
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
 */
open class KoinMviRetainedStore<S : State>(
    features: Set<Feature<S>>,
    defaultState: S,
    dispatcherConfig: CoroutineDispatcherConfig = DefaultCoroutineDispatcherConfig(),
) : MviRetainedStore<S, Any>() {
    override val dispatcherConfig: CoroutineDispatcherConfig = dispatcherConfig
    override val store: MviStore<S, Any> = MviRuntime(
        features = features,
        defaultState = defaultState,
        dispatcherConfig = dispatcherConfig,
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
 * Registers a [KoinMviRetainedStore] for state type [S] as a Koin ViewModel, qualified by [stateQualifier].
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
 *     mviStore(defaultState = CounterState()) {
 *         add(get<AutoDecreaseFeature>())   // FlowFeature — passed through
 *         add(get<DecreaseFeature>())       // FunctionTypedFeature — wrapped automatically
 *     }
 * }
 * ```
 */
inline fun <reified S : State> Module.mviStore(
    defaultState: S,
    dispatcherConfig: CoroutineDispatcherConfig = DefaultCoroutineDispatcherConfig(),
    crossinline features: FeatureRegistrar<S>.() -> Unit,
) {
    viewModel(stateQualifier<S>()) {
        val registrar = FeatureRegistrar<S>(this)
        registrar.features()
        KoinMviRetainedStore(
            features = registrar.build(),
            defaultState = defaultState,
            dispatcherConfig = dispatcherConfig,
        )
    }
}

/**
 * Returns the [KoinMviRetainedStore] for state type [S] scoped to the current navigation back-stack entry.
 *
 * The qualifier is derived from [S] automatically — no string literals needed at call sites.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun CounterScreen(store: KoinMviRetainedStore<CounterState> = koinMviStore()) { ... }
 * ```
 */
@Composable
inline fun <reified S : State> koinMviStore(): KoinMviRetainedStore<S> =
    koinViewModel(stateQualifier<S>())
