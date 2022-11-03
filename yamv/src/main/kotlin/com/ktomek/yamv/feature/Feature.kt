package com.ktomek.yamv.feature

import com.ktomek.yamv.state.Store
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

val Feature<*>.featureScope: CoroutineScope
    get() = internalFeatureScope

/**
 * Single processor will produce [OUTCOME] based on received intention.
 */
abstract class Feature<OUTCOME> : IFeature by DefaultFeature() {

    /**
     * Receive Intention and produce [OUTCOME]
     * @param intention: intention
     */
    abstract suspend operator fun invoke(intention: Any, store: Store): OUTCOME

    open val dispatcher: CoroutineDispatcher?
        get() = null
}
