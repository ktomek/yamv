package com.ktomek.yamv.processor.fixtures.wrap

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.annotations.AutoState
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.ActionTypedFeature
import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.feature.TypedFeature
import com.ktomek.yamv.feature.TypedUnitFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AutoState
data class WrapState(val value: Int = 0) : State

data object WrapFunctionIntention
data object WrapTypedIntention
data object WrapActionIntention
data object WrapTypedUnitIntention

@AutoFeature
class WrapFunctionTypedFeature @Inject constructor() :
    FunctionTypedFeature<WrapState, WrapFunctionIntention> {
    override suspend fun invoke(intention: WrapFunctionIntention): Outcome<WrapState> =
        StateOutcome { it }
}

@AutoFeature
class WrapTypedFeatureClass @Inject constructor() :
    TypedFeature<WrapState, WrapTypedIntention> {
    override fun invoke(intention: Flow<WrapTypedIntention>): Flow<Outcome<WrapState>> =
        emptyFlow()
}

@AutoFeature
class WrapActionTypedFeature @Inject constructor() :
    ActionTypedFeature<WrapState, WrapActionIntention> {
    override suspend fun invoke(intention: WrapActionIntention) = Unit
}

@AutoFeature
class WrapTypedUnitFeatureClass @Inject constructor() :
    TypedUnitFeature<WrapState, WrapTypedUnitIntention> {
    override fun invoke(intention: Flow<WrapTypedUnitIntention>): Flow<Unit> =
        intention.map { }
}
