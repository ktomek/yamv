package com.ktomek.yamv.processor.fixtures.binds

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.annotations.AutoState
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

@AutoState
data class BindsState(val value: Int = 0) : State

data object BindsFlowIntention
data object BindsFlowUnitIntention

@AutoFeature
class BindsFlowFeature @Inject constructor() : Feature.FlowFeature<BindsState> {
    override fun invoke(intentions: Flow<Any>): Flow<Outcome<BindsState>> = emptyFlow()
}

@AutoFeature
class BindsFlowUnitFeature @Inject constructor() : Feature.FlowUnitFeature<BindsState> {
    override fun invoke(intentions: Flow<Any>): Flow<Unit> = emptyFlow()
}
