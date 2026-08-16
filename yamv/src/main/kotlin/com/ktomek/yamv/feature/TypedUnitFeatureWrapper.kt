package com.ktomek.yamv.feature

import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance

inline fun <S : State, reified INTENTION> TypedUnitFeature<S, INTENTION>.wrap(): Feature<S> =
    object : Feature.FlowUnitFeature<S>, WrappedFeature {
        override fun invoke(intentions: Flow<Any>): Flow<Unit> =
            channelFlow {
                val typedIntentions = intentions.filterIsInstance<INTENTION>()
                this@wrap.invoke(typedIntentions).collect(::send)
            }
    }
