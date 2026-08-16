package com.ktomek.yamv.intention

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.Feature.FlowFeature
import com.ktomek.yamv.feature.Feature.FlowUnitFeature
import com.ktomek.yamv.feature.HasFeatureDispatcher
import com.ktomek.yamv.feature.HasFeatureScope
import com.ktomek.yamv.feature.TypedFeatureHolder
import com.ktomek.yamv.logging.Yamv
import com.ktomek.yamv.logging.YamvLogLevel
import com.ktomek.yamv.state.CoroutineDispatcherConfig
import com.ktomek.yamv.state.ErrorSource
import com.ktomek.yamv.state.MviErrorContext
import com.ktomek.yamv.state.MviExceptionHandler
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

/**
 * Base MVI router which is taking intentions and sending them to features.
 */
internal class FeatureRouter<S : State>(
    private val features: Set<Feature<S>>,
) : IntentionRouter<S> {

    private lateinit var featureJobs: List<Job>
    private lateinit var exceptionHandler: MviExceptionHandler
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

    override fun initialize(
        scope: CoroutineScope,
        dispatcherConfig: CoroutineDispatcherConfig,
        exceptionHandler: MviExceptionHandler,
    ) {
        check(!isInitialized.getAndSet(true)) {
            "Router has already been initialized"
        }
        this.exceptionHandler = exceptionHandler

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
            val dispatcher = (f as? HasFeatureDispatcher)?.featureDispatcher
                ?: dispatcherConfig.provideFeatureDispatcher(f)
            scope.launch(dispatcher) {
                // Mark this feature toward the readiness gate exactly once, via whichever happens
                // first: it subscribes to intentionFlow, or its flow completes without subscribing.
                val marked: AtomicBoolean = atomic(false)
                val markOnce = { if (!marked.getAndSet(true)) markSubscribed() }
                try {
                    processFeature(feature, markOnce)
                    // Reached only if the feature flow completed normally without ever subscribing
                    // to intentionFlow (e.g. an eager `flowOf(...)` feature). Mark it so the gate
                    // does not wait forever on a feature that will never subscribe.
                    markOnce()
                } catch (e: CancellationException) {
                    throw e
                } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                    Yamv.log(YamvLogLevel.ERROR, TAG, "Feature $feature failed: $e")
                    exceptionHandler.handle(
                        MviErrorContext(source = ErrorSource.FEATURE, feature = feature),
                        e,
                    )
                }
            }
        }
    }

    override fun observeOutcomes(): SharedFlow<Outcome<S>> {
        if (isDisposed.value) error("Router has been disposed")
        return outcomeFlow.asSharedFlow()
    }

    /**
     * Collects [feature], invoking [markOnce] the moment it actually subscribes to [intentionFlow]
     * (via [onSubscription] on the gated flow handed to it). This is what fixes the
     * dropped-first-intention race (#69): the typed-feature wrappers subscribe to [intentionFlow]
     * inside a `channelFlow`, strictly after the outer flow starts, so the previous
     * `onStart`-on-the-outer-flow signal opened the readiness gate before the feature was actually
     * collecting — and an intention emitted in that window was dropped by the `replay = 0`
     * SharedFlow.
     *
     * The signal must ride on [intentionFlow] itself (not on an operator wrapped around the
     * feature's returned flow, e.g. `onCompletion`, which perturbs the collector and reintroduces
     * the race). Features that never subscribe are marked by the caller once this function returns.
     */
    private suspend fun processFeature(feature: Feature<S>, markOnce: () -> Unit) {
        val gated = intentionFlow.onSubscription { markOnce() }
        when (feature) {
            is FlowFeature<S> -> feature(gated)
                .filterNotNull()
                .collect(outcomeFlow::emit)

            is FlowUnitFeature<S> -> feature(gated)
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

        featureJobs.forEach(Job::cancel)
    }

    fun isDisposed(): Boolean = isDisposed.value

    companion object {
        private const val TAG = "FeatureRouter"
    }
}
