package com.ktomek.yamv.feature

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.state.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.invoke

inline fun <reified S : State, reified INTENTION> TypedFeature<S, INTENTION>.wrap(): FeatureFlow<S> =
    object : FeatureFlow<S>() {
        override suspend operator fun invoke(intentions: Flow<Any>, store: Store): Flow<Outcome<S>> =
            channelFlow {
                this@wrap.dispatcher?.invoke {
                    intentions
                        .filterIsInstance<INTENTION>()
                        .map { intention -> this@wrap.invoke(intention, store) }
                        .collect(::send)
                } ?: intentions
                    .filterIsInstance<INTENTION>()
                    .map { intention -> this@wrap.invoke(intention, store) }
                    .collect(::send)
            }

        override fun close() {
            super.close()
            this@wrap.close()
        }
    }
