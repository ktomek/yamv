package com.ktomek.yamv.feature

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Opt-in interface for features that need to run on a specific [CoroutineDispatcher].
 *
 * When a feature implements this interface, [FeatureRouter][com.ktomek.yamv.intention.FeatureRouter]
 * launches the feature on [featureDispatcher] instead of the dispatcher provided by
 * [CoroutineDispatcherConfig][com.ktomek.yamv.state.CoroutineDispatcherConfig].
 *
 * ```kotlin
 * class NetworkFeature : TypedFeature<MyState, FetchData>, HasFeatureDispatcher {
 *     override val featureDispatcher = Dispatchers.IO
 *     // ...
 * }
 * ```
 *
 * [HasFeatureScope] extends this interface, so features with a custom scope automatically
 * expose the scope's dispatcher.
 */
interface HasFeatureDispatcher {
    val featureDispatcher: CoroutineDispatcher
}
