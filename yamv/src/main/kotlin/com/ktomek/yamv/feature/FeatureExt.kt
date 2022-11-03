package com.ktomek.yamv.feature

import com.ktomek.yamv.state.Store

inline fun <reified OUTCOME, reified INTENTION> typedFeature(
    crossinline feature: suspend (INTENTION, store: Store) -> OUTCOME
): TypedFeature<OUTCOME, INTENTION> =
    object : TypedFeature<OUTCOME, INTENTION>() {
        override suspend fun invoke(intention: INTENTION, store: Store): OUTCOME =
            feature(intention, store)
    }
