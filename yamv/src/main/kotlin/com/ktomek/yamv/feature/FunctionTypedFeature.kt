package com.ktomek.yamv.feature

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State

/**
 * Abstract class representing a typed feature that processes intentions and produces outcomes.
 *
 * @param S The type of the outcome produced by the feature.
 * @param INTENTION The type of the intention processed by the feature.
 */
fun interface FunctionTypedFeature<S : State, INTENTION> {
    /**
     * Receives an intention and produces an outcome.
     *
     * @param intention The intention to be processed.
     * @return The outcome produced by the feature function.
     */
    suspend operator fun invoke(intention: INTENTION): Outcome<S>
}
