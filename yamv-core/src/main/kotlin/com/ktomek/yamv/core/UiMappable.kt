package com.ktomek.yamv.core

/**
 * Opt-in interface for State classes that declare a UI projection.
 *
 * Implementing this allows using [MviStore.uiState()] for type-safe
 * State → UiState mapping with [distinctUntilChanged] to minimize
 * Compose recompositions.
 *
 * @param UiS the UI state type — typically a subset of the full State
 */
interface UiMappable<UiS : Any> {
    fun toUiState(): UiS
}
