package com.ktomek.yamv.state

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.IntentionOutcome
import com.ktomek.yamv.core.Reducer
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.intention.FeatureRouter
import com.ktomek.yamv.intention.IntentionRouter
import com.ktomek.yamv.logging.Yamv
import com.ktomek.yamv.logging.YamvLogLevel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MviRuntime<S : State>(
    private val intentionRouter: IntentionRouter<S>,
    private val dispatcherConfig: CoroutineDispatcherConfig,
    val defaultState: S,
    private val exceptionHandler: MviExceptionHandler = MviExceptionHandler.Default,
) : MviStore<S, Any> {

    private val scope = CoroutineScope(
        SupervisorJob() +
            dispatcherConfig.provideReducerDispatcher() +
            CoroutineExceptionHandler { _, throwable ->
                Yamv.log(YamvLogLevel.ERROR, TAG, "Unhandled exception in scope: $throwable")
            },
    )

    private val effectsFlow: MutableSharedFlow<EffectOutcome<S>> = MutableSharedFlow()
    override val effects: Flow<EffectOutcome<S>>
        get() = effectsFlow

    private val stateFlow: MutableStateFlow<S> = MutableStateFlow(defaultState)
    override val state: StateFlow<S>
        get() = stateFlow

    init {
        Yamv.log(YamvLogLevel.DEBUG, TAG, "MviRuntime created with defaultState=$defaultState")
        intentionRouter.initialize(scope, dispatcherConfig, exceptionHandler)

        scope.launch(dispatcherConfig.provideReducerDispatcher()) {
            intentionRouter
                .observeOutcomes()
                .filterIsInstance<Reducer<S>>()
                .scan(defaultState) { state, reducer ->
                    try {
                        reducer.reduce(state)
                    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                        handleException(MviErrorContext(source = ErrorSource.REDUCER), e)
                        state
                    }
                }
                .collect { newState ->
                    Yamv.log(YamvLogLevel.VERBOSE, TAG, "State updated: $newState")
                    stateFlow.update { newState }
                }
        }

        scope.launch(dispatcherConfig.provideReducerDispatcher()) {
            intentionRouter
                .observeOutcomes()
                .filterIsInstance<EffectOutcome<S>>()
                .collect { effect ->
                    try {
                        Yamv.log(YamvLogLevel.DEBUG, TAG, "Effect emitted: $effect")
                        effectsFlow.emit(effect)
                    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                        handleException(MviErrorContext(source = ErrorSource.EFFECT), e)
                    }
                }
        }

        scope.launch(dispatcherConfig.provideIntentionDispatcher(null)) {
            intentionRouter
                .observeOutcomes()
                .filterIsInstance<IntentionOutcome<S>>()
                .map { it.intention }
                .collect { intention ->
                    try {
                        intentionRouter.dispatchIntention(intention)
                    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                        handleException(
                            MviErrorContext(
                                source = ErrorSource.INTENTION_REDISPATCH,
                                intention = intention,
                            ),
                            e,
                        )
                    }
                }
        }
    }

    override fun dispatch(intention: Any) {
        Yamv.log(YamvLogLevel.VERBOSE, TAG, "dispatch($intention)")
        scope.launch(dispatcherConfig.provideIntentionDispatcher(intention)) {
            intentionRouter.dispatchIntention(intention)
        }
    }

    override fun clear() {
        Yamv.log(YamvLogLevel.INFO, TAG, "MviRuntime cleared")
        scope.cancel()
    }

    /**
     * Delegates to [exceptionHandler]. If the handler rethrows (default),
     * the entire scope is cancelled for fail-fast behavior.
     */
    private fun handleException(context: MviErrorContext, exception: Throwable) {
        try {
            exceptionHandler.handle(context, exception)
        } catch (@Suppress("TooGenericExceptionCaught") rethrown: Throwable) {
            scope.cancel()
            throw rethrown
        }
    }

    companion object {
        private const val TAG = "MviRuntime"
    }
}

/**
 * Factory function to create an MviRuntime from a set of features.
 *
 * @param features The set of features to attach to this runtime.
 * @param defaultState The initial state.
 * @param dispatcherConfig The coroutine dispatcher configuration (defaults to DefaultCoroutineDispatcherConfig).
 * @param exceptionHandler The exception handler (defaults to [MviExceptionHandler.Default] which rethrows).
 * @return A new MviRuntime instance.
 */
fun <S : State> MviRuntime(
    features: Set<Feature<S>>,
    defaultState: S,
    dispatcherConfig: CoroutineDispatcherConfig = DefaultCoroutineDispatcherConfig(),
    exceptionHandler: MviExceptionHandler = MviExceptionHandler.Default,
): MviRuntime<S> =
    MviRuntime(
        intentionRouter = FeatureRouter(features),
        dispatcherConfig = dispatcherConfig,
        defaultState = defaultState,
        exceptionHandler = exceptionHandler,
    )
