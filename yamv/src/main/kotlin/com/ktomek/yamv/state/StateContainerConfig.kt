package com.ktomek.yamv.state

import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

data class StateContainerConfig<S : State>(
    internal val features: Set<Feature<S>>,
    internal val dispatcher: CoroutineDispatcher,
    internal val defaultState: S,
)

/**
 * Builder for StateContainerConfig to enable DSL-style construction.
 */
class StateContainerBuilder<S : State> {
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
     * Build the final StateContainerConfig.
     */
    fun build(): StateContainerConfig<S> = StateContainerConfig(
        features = features,
        dispatcher = dispatcher,
        defaultState = defaultState
    )
}

/**
 * DSL entry point for creating StateContainerConfig.
 */
fun <S : State> stateContainerConfig(block: StateContainerBuilder<S>.() -> Unit): StateContainerConfig<S> =
    StateContainerBuilder<S>().apply(block).build()
