package com.ktomek.yamv.feature

import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

inline fun <reified S : State, reified INTENTION> ActionTypedFeature<S, INTENTION>.wrap(): Feature<S> =
    Feature.FlowUnitFeature<S> { intentions ->
        channelFlow {
            intentions
                .filterIsInstance<INTENTION>()
                .map(this@wrap::invoke)
                .collect {}
        }
    }
