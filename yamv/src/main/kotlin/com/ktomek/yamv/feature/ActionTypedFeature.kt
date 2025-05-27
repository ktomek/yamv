package com.ktomek.yamv.feature

import com.ktomek.yamv.core.State

/**
 * Abstract class representing a typed feature that processes intentions and produces outcomes.
 *
 * @param S The type of the outcome produced by the feature.
 * @param INTENTION The type of the intention processed by the feature.
 */
fun interface ActionTypedFeature<S : State, INTENTION> {
    /**
     * Receives an intention and produces an outcome.
     *
     * @param intention The intention to be processed.
     * @param store The store to be used in the feature function.
     * @return The outcome produced by the feature function.
     */
    suspend operator fun invoke(intention: INTENTION)
}
