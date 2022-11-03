package com.ktomek.yamv.feature

import com.ktomek.yamv.state.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.invoke

inline fun <reified OUTCOME, reified INTENTION> TypedFeature<OUTCOME, INTENTION>.wrap(): FeatureFlow<OUTCOME> =
    object : FeatureFlow<OUTCOME>() {
        override suspend operator fun invoke(intentions: Flow<Any>, store: Store): Flow<OUTCOME> =
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
