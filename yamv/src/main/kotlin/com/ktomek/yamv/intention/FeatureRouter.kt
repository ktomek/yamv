package com.ktomek.yamv.intention

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.Feature.FlowFeature
import com.ktomek.yamv.feature.Feature.FlowUnitFeature
import com.ktomek.yamv.feature.HasFeatureScope
import com.ktomek.yamv.feature.TypedFeatureHolder
import com.ktomek.yamv.logging.Yamv
import com.ktomek.yamv.logging.YamvLogLevel
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic

/**
 * Base MVI router which is taking intentions and sending them to features.
 */
internal class FeatureRouter<S : State>(
    private val features: Set<Feature<S>>,
) : IntentionRouter<S> {

    private lateinit var featureJobs: List<Job>
    private val outcomeFlow = MutableSharedFlow<Outcome<S>>()
    private val intentionFlow = MutableSharedFlow<Any>(extraBufferCapacity = 64)

    private val isInitialized: AtomicBoolean = atomic(false)
    private val isDisposed: AtomicBoolean = atomic(false)

    private val subscribed = CompletableDeferred<Unit>()
    private val remainingToSubscribe: AtomicInt = atomic(features.size)

    override suspend fun dispatchIntention(intention: Any) {
        if (isDisposed.value) error("Router has been disposed")
        if (!isInitialized.value) error("Router has not been initialized")
        subscribed.await()
        Yamv.log(YamvLogLevel.VERBOSE, TAG, "Dispatching intention: $intention")
        intentionFlow.emit(intention)
    }

    override fun initialize(scope: CoroutineScope, dispatcherConfig: CoroutineDispatcherConfig) {
        check(!isInitialized.getAndSet(true)) {
            "Router has already been initialized"
        }

        Yamv.log(YamvLogLevel.INFO, TAG, "Initializing FeatureRouter with ${features.size} feature(s)")

        if (features.isEmpty()) {
            subscribed.complete(Unit)
            Yamv.log(YamvLogLevel.DEBUG, TAG, "No features — router ready immediately")
            return
        }

        scope.coroutineContext[Job]?.invokeOnCompletion {
            features.forEach { feature ->
                val f = (feature as? TypedFeatureHolder)?.feature ?: feature
                (f as? HasFeatureScope)?.featureScope?.cancel()
            }
        }

        featureJobs = features.map { feature ->
            val f = (feature as? TypedFeatureHolder)?.feature ?: feature
            scope.launch(dispatcherConfig.provideFeatureDispatcher(f)) {
                processFeature(feature)
            }
        }
    }

    override fun observeOutcomes(): SharedFlow<Outcome<S>> {
        if (isDisposed.value) error("Router has been disposed")
        if (!isInitialized.value) error("Router has not been initialized")
        return outcomeFlow.asSharedFlow()
    }

    private suspend fun processFeature(feature: Feature<S>) {
        when (feature) {
            is FlowFeature<S> -> feature(intentionFlow)
                .onStart { markSubscribed() }
                .filterNotNull()
                .collect(outcomeFlow::emit)

            is FlowUnitFeature<S> -> feature(intentionFlow)
                .onStart { markSubscribed() }
                .collect { }
        }
    }

    private fun markSubscribed() {
        val remaining = remainingToSubscribe.decrementAndGet()
        Yamv.log(YamvLogLevel.DEBUG, TAG, "Feature subscribed. Remaining: $remaining")
        if (remaining == 0 && !subscribed.isCompleted) {
            subscribed.complete(Unit)
            Yamv.log(YamvLogLevel.INFO, TAG, "All features subscribed — router ready")
        }
    }

    fun shutdown() {
        if (isDisposed.getAndSet(true)) return
        Yamv.log(YamvLogLevel.INFO, TAG, "Shutting down FeatureRouter")

        features.forEach { feature ->
            val f = (feature as? TypedFeatureHolder)?.feature ?: feature
            (f as? HasFeatureScope)?.featureScope?.cancel()
        }

        featureJobs.forEach { job ->
            try {
                job.cancel()
            } catch (e: Exception) {
                Yamv.log(YamvLogLevel.WARN, TAG, "Error cancelling feature job: ${e.message}")
            }
        }
    }

    fun isDisposed(): Boolean = isDisposed.value

    companion object {
        private const val TAG = "FeatureRouter"
    }
}
