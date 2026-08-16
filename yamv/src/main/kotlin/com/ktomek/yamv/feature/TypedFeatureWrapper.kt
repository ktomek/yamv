package com.ktomek.yamv.feature

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance

inline fun <S : State, reified INTENTION> TypedFeature<S, INTENTION>.wrap(): Feature<S> =
    object : Feature.FlowFeature<S>, WrappedFeature {
        override fun invoke(intentions: Flow<Any>): Flow<Outcome<S>> =
            channelFlow {
                val typedIntentions = intentions.filterIsInstance<INTENTION>()
                this@wrap.invoke(typedIntentions).collect(::send)
            }
    }
