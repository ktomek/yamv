package com.ktomek.yamv.processor.fixtures.property

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.annotations.AutoState
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.feature.ActionTypedFeature
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.FunctionTypedFeature
import com.ktomek.yamv.feature.TypedFeature
import com.ktomek.yamv.feature.TypedUnitFeature
import com.ktomek.yamv.feature.functionTypedFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

@AutoState
data class PropertyState(val value: Int = 0) : State

data object PropertyFunctionIntention
data object PropertyTypedIntention
data object PropertyActionIntention
data object PropertyTypedUnitIntention

@AutoFeature
val propertyAlreadyWrappedFeature: Feature<PropertyState> =
    functionTypedFeature<PropertyState, PropertyFunctionIntention> { StateOutcome { it } }

@AutoFeature
val propertyFunctionTypedFeature: FunctionTypedFeature<PropertyState, PropertyFunctionIntention> =
    FunctionTypedFeature { StateOutcome { it } }

@AutoFeature
val propertyTypedFeature: TypedFeature<PropertyState, PropertyTypedIntention> =
    TypedFeature { _: Flow<PropertyTypedIntention> -> emptyFlow<Outcome<PropertyState>>() }

@AutoFeature
val propertyActionTypedFeature: ActionTypedFeature<PropertyState, PropertyActionIntention> =
    ActionTypedFeature { }

@AutoFeature
val propertyTypedUnitFeature: TypedUnitFeature<PropertyState, PropertyTypedUnitIntention> =
    TypedUnitFeature { it.map { } }
