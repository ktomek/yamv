package com.ktomek.yamv.profile.logic.feature

import com.ktomek.yamv.annotations.AutoFeature
import com.ktomek.yamv.feature.Feature.FlowFeature
import com.ktomek.yamv.profile.logic.SaveProfileIntention
import com.ktomek.yamv.profile.logic.outcome.ProfileOutcome
import com.ktomek.yamv.profile.logic.outcome.SavingOutcome
import com.ktomek.yamv.profile.logic.outcome.ValidationTickOutcome
import com.ktomek.yamv.profile.logic.state.ProfileState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Simulates a save with retry/validation bookkeeping.
 *
 * A single [SaveProfileIntention] produces many state emissions — `isSaving` flips
 * on, several [ValidationTickOutcome]s churn internal bookkeeping, then `isSaving`
 * flips off. Only the two `isSaving` transitions change the UI projection, so a
 * screen collecting `uiState()` recomposes twice while a screen collecting the
 * full state recomposes on every tick.
 */
@AutoFeature
class SaveProfileFeature @Inject constructor() : FlowFeature<ProfileState> {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(intentions: Flow<Any>): Flow<ProfileOutcome> =
        intentions
            .filterIsInstance<SaveProfileIntention>()
            .flatMapLatest { saveFlow() }

    private fun saveFlow(): Flow<ProfileOutcome> = flow {
        emit(SavingOutcome(true))
        repeat(VALIDATION_PASSES) { pass ->
            delay(VALIDATION_DELAY_MS)
            emit(ValidationTickOutcome(token = pass.toLong()))
        }
        emit(SavingOutcome(false))
    }

    private companion object {
        const val VALIDATION_PASSES = 5
        const val VALIDATION_DELAY_MS = 150L
    }
}
