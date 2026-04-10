package com.ktomek.yamv.feature

import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance

inline fun <S : State, reified INTENTION> TypedFeature<S, INTENTION>.wrap(): Feature<S> =
    Feature.FlowFeature<S> { intentions ->
        channelFlow {
            val typedIntentions = intentions.filterIsInstance<INTENTION>()
            this@wrap.invoke(typedIntentions).collect(::send)
        }
    }
