package com.ktomek.yamv.feature

import com.ktomek.yamv.state.Store
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

val FeatureFlow<*>.featureScope: CoroutineScope
    get() = internalFeatureScope

/**
 * Single processor will produce [OUTCOME] based on received intentions.
 */
abstract class FeatureFlow<OUTCOME> : IFeature by DefaultFeature() {

    lateinit var store: Store
        internal set

    /**
     * Observe intentions and produce [OUTCOME]
     * @param intentions: stream of intentions
     */
    abstract suspend operator fun invoke(intentions: Flow<Any>, store: Store): Flow<OUTCOME>

    open val dispatcher: CoroutineDispatcher?
        get() = null
}
