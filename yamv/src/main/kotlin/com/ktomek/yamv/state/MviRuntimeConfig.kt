package com.ktomek.yamv.state

import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import kotlinx.coroutines.CoroutineDispatcher

data class MviRuntimeConfig<S : State>(
    internal val features: Set<Feature<S>>,
    internal val dispatcher: CoroutineDispatcher,
    internal val defaultState: S,
)

/**
 * Builder for MviRuntimeConfig to enable DSL-style construction.
 */
class MviRuntimeBuilder<S : State> {
    private val features = mutableSetOf<Feature<S>>()
    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var defaultState: S

    /**
     * Add a single feature.
     */
    fun feature(feature: Feature<S>) = apply { features.add(feature) }

    /**
     * Add multiple features.
     */
    fun features(featureSet: Set<Feature<S>>) = apply { features.addAll(featureSet) }

    /**
     * Set the CoroutineDispatcher.
     */
    fun dispatcher(dispatcher: CoroutineDispatcher) = apply { this.dispatcher = dispatcher }

    /**
     * Set the default state.
     */
    fun defaultState(state: S) = apply { defaultState = state }

    /**
     * Build the final MviRuntimeConfig.
     */
    fun build(): MviRuntimeConfig<S> = MviRuntimeConfig(
        features = features,
        dispatcher = dispatcher,
        defaultState = defaultState
    )
}

/**
 * DSL entry point for creating MviRuntimeConfig.
 */
fun <S : State> mviRuntimeConfig(block: MviRuntimeBuilder<S>.() -> Unit): MviRuntimeConfig<S> =
    MviRuntimeBuilder<S>().apply(block).build()
