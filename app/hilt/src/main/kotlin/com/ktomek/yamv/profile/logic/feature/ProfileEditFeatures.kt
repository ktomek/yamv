package com.ktomek.yamv.profile.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.feature.functionTypedFeature
import com.ktomek.yamv.profile.logic.UpdateEmailIntention
import com.ktomek.yamv.profile.logic.UpdateNameIntention
import com.ktomek.yamv.profile.logic.outcome.SetEmailOutcome
import com.ktomek.yamv.profile.logic.outcome.SetNameOutcome
import com.ktomek.yamv.profile.logic.state.ProfileState

@AutoFeature
val updateNameFeature = functionTypedFeature<ProfileState, UpdateNameIntention> {
    SetNameOutcome(it.value)
}

@AutoFeature
val updateEmailFeature = functionTypedFeature<ProfileState, UpdateEmailIntention> {
    SetEmailOutcome(it.value)
}
