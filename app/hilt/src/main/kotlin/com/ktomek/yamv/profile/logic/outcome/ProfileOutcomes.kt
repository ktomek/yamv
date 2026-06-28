package com.ktomek.yamv.profile.logic.outcome

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.StateOutcome
import com.ktomek.yamv.profile.logic.state.ProfileState

typealias ProfileOutcome = Outcome<ProfileState>
typealias ProfileReducer = StateOutcome<ProfileState>

data class SetNameOutcome(val value: String) : ProfileReducer {
    override fun reduce(prevState: ProfileState): ProfileState =
        prevState.copy(name = value)
}

data class SetEmailOutcome(val value: String) : ProfileReducer {
    override fun reduce(prevState: ProfileState): ProfileState =
        prevState.copy(email = value)
}

data class SavingOutcome(val isSaving: Boolean) : ProfileReducer {
    override fun reduce(prevState: ProfileState): ProfileState =
        prevState.copy(isSaving = isSaving)
}

/**
 * Internal-only churn: bumps bookkeeping fields the UI never observes. Emitting
 * this re-emits state, but `ProfileState.toUiState()` is unchanged — so a
 * projected collector does not recompose.
 */
data class ValidationTickOutcome(val token: Long) : ProfileReducer {
    override fun reduce(prevState: ProfileState): ProfileState =
        prevState.copy(
            saveAttempts = prevState.saveAttempts + 1,
            lastValidationToken = token,
        )
}
