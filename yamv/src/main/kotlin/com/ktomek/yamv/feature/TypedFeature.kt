package com.ktomek.yamv.feature

import com.ktomek.yamv.state.Store
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

abstract class TypedFeature<OUTCOME, INTENTION> : IFeature by DefaultFeature() {
    /**
     * Receive Intention and produce [OUTCOME]
     * @param intention: intention
     */
    abstract suspend operator fun invoke(intention: INTENTION, store: Store): OUTCOME

    val featureScope: CoroutineScope
        get() = internalFeatureScope

    open val dispatcher: CoroutineDispatcher?
        get() = null
}
