package com.ktomek.yamv.state

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.IntentionOutcome
import com.ktomek.yamv.core.Reducer
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.intention.FeatureRouter
import com.ktomek.yamv.intention.IntentionRouter
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
) : MviStore<S, Any> {

    private val scope = CoroutineScope(SupervisorJob() + dispatcherConfig.provideReducerDispatcher())

    private val effectsFlow: MutableSharedFlow<EffectOutcome<S>> = MutableSharedFlow()
    override val effects: Flow<EffectOutcome<S>>
        get() = effectsFlow

    private val stateFlow: MutableStateFlow<S> = MutableStateFlow(defaultState)
    override val state: StateFlow<S>
        get() = stateFlow

    init {
        intentionRouter.initialize(scope, dispatcherConfig)

        scope.launch(dispatcherConfig.provideReducerDispatcher()) {
            intentionRouter
                .observeOutcomes()
                .filterIsInstance<Reducer<S>>()
                .scan(defaultState) { state, reducer -> reducer.reduce(state) }
                .collect { newState -> stateFlow.update { newState } }
        }

        scope.launch(dispatcherConfig.provideReducerDispatcher()) {
            intentionRouter
                .observeOutcomes()
                .filterIsInstance<EffectOutcome<S>>()
                .collect(effectsFlow::emit)
        }

        scope.launch(dispatcherConfig.provideIntentionDispatcher(null)) {
            intentionRouter
                .observeOutcomes()
                .filterIsInstance<IntentionOutcome<S>>()
                .map { it.intention }
                .collect(intentionRouter::dispatchIntention)
        }
    }

    override fun dispatch(intention: Any) {
        scope.launch(dispatcherConfig.provideIntentionDispatcher(intention)) {
            intentionRouter.dispatchIntention(intention)
        }
    }

    override fun clear() {
        scope.cancel()
    }
}

/**
 * Factory function to create an MviRuntime from a set of features.
 *
 * @param features The set of features to attach to this runtime.
 * @param defaultState The initial state.
 * @param dispatcherConfig The coroutine dispatcher configuration (defaults to DefaultCoroutineDispatcherConfig).
 * @return A new MviRuntime instance.
 */
fun <S : State> MviRuntime(
    features: Set<Feature<S>>,
    defaultState: S,
    dispatcherConfig: CoroutineDispatcherConfig = DefaultCoroutineDispatcherConfig(),
): MviRuntime<S> =
    MviRuntime(
        intentionRouter = FeatureRouter(features),
        dispatcherConfig = dispatcherConfig,
        defaultState = defaultState,
    )
