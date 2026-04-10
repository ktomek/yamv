package com.ktomek.yamv.feature

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.ContinuationInterceptor

/**
 * Opt-in interface for features that need a coroutine scope tied to the feature's lifetime.
 *
 * Extends [HasFeatureDispatcher] — the dispatcher is extracted from [featureScope]'s context
 * by default, so [FeatureRouter][com.ktomek.yamv.intention.FeatureRouter] launches the feature
 * on the scope's dispatcher automatically.
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
interface HasFeatureScope : HasFeatureDispatcher {
    val featureScope: CoroutineScope

    override val featureDispatcher: CoroutineDispatcher
        get() = featureScope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
            ?: Dispatchers.Default
}
