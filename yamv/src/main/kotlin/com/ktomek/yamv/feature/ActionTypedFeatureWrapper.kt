package com.ktomek.yamv.feature

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlin.invoke

inline fun <reified S : State, reified INTENTION> ActionTypedFeature<S, INTENTION>.wrap(): Feature<S> =
    object : TypedUnitFeatureHolder<S> {
        override val feature: Any = this@wrap

        override fun invoke(intentions: Flow<Any>): Flow<Outcome<S>> =
            channelFlow {
                intentions
                    .filterIsInstance<INTENTION>()
                    .map(this@wrap::invoke)
                    .collect {}
            }
    }
