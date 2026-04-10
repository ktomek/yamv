package com.ktomek.yamv.feature

import kotlinx.coroutines.CoroutineScope

/**
 * Opt-in interface for features that need a coroutine scope tied to the feature's lifetime.
 *
 * Implement via delegation to [DefaultFeatureScope]:
 * ```kotlin
 * class MyFeature : Feature.FlowFeature<MyState>, HasFeatureScope by DefaultFeatureScope() {
 *     override fun invoke(intentions: Flow<Any>) = channelFlow {
 *         featureScope.launch { /* background work */ }
 *         intentions.collect { /* handle intentions */ }
 *     }
 * }
 * ```
 *
 * The scope is automatically cancelled when `MviRuntime.clear()` is called.
 */
interface HasFeatureScope {
    val featureScope: CoroutineScope
}
