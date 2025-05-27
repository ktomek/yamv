package com.ktomek.yamv.feature

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow

sealed interface Feature<S: State> {
    fun interface FlowFeature<S: State> : Feature<S> {
        operator fun invoke(intentions: Flow<Any>): Flow<Outcome<S>>
    }

    fun interface FlowUnitFeature<S: State> : Feature<S> {
        operator fun invoke(intentions: Flow<Any>): Flow<Unit>
    }
}