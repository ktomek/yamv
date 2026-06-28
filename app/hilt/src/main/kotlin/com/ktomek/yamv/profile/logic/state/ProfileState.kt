package com.ktomek.yamv.profile.logic.state

import com.ktomek.yamv.annotations.AutoState
import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.UiMappable
import com.ktomek.yamv.profile.logic.uistate.ProfileUiState

/**
 * Full business-logic state for the profile screen.
 *
 * Note the split between UI-relevant fields ([name], [email], [isSaving]) and
 * internal bookkeeping ([saveAttempts], [lastValidationToken]) that the UI must
 * never observe. The bookkeeping fields churn during a save, re-emitting state,
 * but they are intentionally excluded from [ProfileUiState] — so a screen that
 * collects [toUiState] does not recompose when only bookkeeping changes.
 */
@AutoState
data class ProfileState(
    val name: String = "",
    val email: String = "",
    val isSaving: Boolean = false,
    // Internal bookkeeping — deliberately NOT part of the UI projection.
    val saveAttempts: Int = 0,
    val lastValidationToken: Long = 0L,
) : State, UiMappable<ProfileUiState> {

    override fun toUiState(): ProfileUiState = ProfileUiState(
        name = name,
        email = email,
        isSaving = isSaving,
        canSave = name.isNotBlank() && email.contains("@") && !isSaving,
    )
}
