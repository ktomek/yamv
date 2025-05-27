package com.ktomek.yamv.intention

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Base MVI dispatcher which is taking intentions and sending them to actions.
 */
class DefaultIntentionDispatcher<S : State>(
    private val features: Set<@JvmSuppressWildcards Feature<S>>,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) : IntentionDispatcher<S> {

    private val outcomeFlow = MutableSharedFlow<Outcome<S>>()

    private val intentionFlow = MutableSharedFlow<Any>(extraBufferCapacity = 64)

    init {
        features.forEach { feature ->
            scope.launch(dispatcher) {
                processFeature(feature)
            }
        }
    }

    private suspend fun processFeature(feature: @JvmSuppressWildcards Feature<S>) {
        when (feature) {
            is Feature.FlowFeature<S> -> feature(intentionFlow).collect(outcomeFlow::emit)
            is Feature.FlowUnitFeature<S> -> feature(intentionFlow).collect { }
        }
    }

    override suspend fun dispatchIntention(intention: Any) {
        intentionFlow.emit(intention)
    }

    override fun observeOutcomes(): SharedFlow<Outcome<S>> = outcomeFlow.asSharedFlow()
}
