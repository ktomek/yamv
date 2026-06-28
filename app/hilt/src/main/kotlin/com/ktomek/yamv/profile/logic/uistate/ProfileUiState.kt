package com.ktomek.yamv.profile.logic.uistate

/**
 * UI-facing projection of `ProfileState`.
 *
 * Holds only what the screen renders, plus the derived [canSave] flag. Internal
 * bookkeeping from the full state is excluded, so collectors only recompose when
 * one of these values actually changes.
 */
data class ProfileUiState(
    val name: String,
    val email: String,
    val isSaving: Boolean,
    val canSave: Boolean,
)
