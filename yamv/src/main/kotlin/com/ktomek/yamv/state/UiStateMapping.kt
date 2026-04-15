package com.ktomek.yamv.state

import com.ktomek.yamv.core.State
import com.ktomek.yamv.core.UiMappable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val DEFAULT_STOP_TIMEOUT_MS = 5_000L

/**
 * Maps a [StateFlow] of [S] to a [StateFlow] of [UiS] with [distinctUntilChanged],
 * so downstream collectors (e.g. Compose) only recompose when the UI-relevant
 * subset of state actually changes.
 *
 * @param scope the [CoroutineScope] used for sharing (e.g. `viewModelScope` or `rememberCoroutineScope()`)
 * @param started sharing strategy, defaults to [SharingStarted.WhileSubscribed] with 5s stop timeout
 * @param mapper transforms the full state into the UI state subset
 */
fun <S : State, UiS : Any> StateFlow<S>.mapToUiState(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(DEFAULT_STOP_TIMEOUT_MS),
    mapper: (S) -> UiS,
): StateFlow<UiS> =
    map(mapper)
        .distinctUntilChanged()
        .stateIn(scope, started, mapper(value))

/**
 * Type-safe shorthand for [mapToUiState] when the State implements [UiMappable].
 *
 * @param scope the [CoroutineScope] used for sharing
 * @param started sharing strategy, defaults to [SharingStarted.WhileSubscribed] with 5s stop timeout
 */
fun <S, UiS : Any> MviStore<S, *>.uiState(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(DEFAULT_STOP_TIMEOUT_MS),
): StateFlow<UiS> where S : State, S : UiMappable<UiS> =
    state.mapToUiState(scope, started) { it.toUiState() }
