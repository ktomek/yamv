package com.ktomek.yamv.intention

import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.Feature.FlowFeature
import com.ktomek.yamv.feature.Feature.FlowUnitFeature
import com.ktomek.yamv.feature.TypedFeatureHolder
import com.ktomek.yamv.state.YamvDispatcherProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base MVI dispatcher which is taking intentions and sending them to features.
 */
class DefaultIntentionDispatcher<S : State>(
    private val features: Set<Feature<S>>,
) : IntentionDispatcher<S> {

    private lateinit var featureJobs: List<Job>
    private val outcomeFlow = MutableSharedFlow<Outcome<S>>()
    private val intentionFlow = MutableSharedFlow<Any>(extraBufferCapacity = 64)

    private val isInitialized: AtomicBoolean = AtomicBoolean(false)
    private val isDisposed: AtomicBoolean = AtomicBoolean(false)

    private val subscribed = CompletableDeferred<Unit>()
    private val remainingToSubscribe = AtomicInteger(features.size)

    override suspend fun dispatchIntention(intention: Any) {
        if (isDisposed.get()) error("Dispatcher has been disposed")
        if (!isInitialized.get()) error("Dispatcher has not been initialized")
        subscribed.await()
        intentionFlow.emit(intention)
    }

    override fun initialize(scope: CoroutineScope, dispatcher: YamvDispatcherProvider) {
        check(!isInitialized.getAndSet(true)) {
            "Dispatcher has already been initialized"
        }

        if (features.isEmpty()) {
            subscribed.complete(Unit)
            return
        }
        
        featureJobs = features.map { feature ->
            val f = (feature as? TypedFeatureHolder)?.feature ?: feature
            scope.launch(dispatcher.provideFeatureDispatcher(f)) {
                processFeature(feature)
            }
        }
    }

    override fun observeOutcomes(): SharedFlow<Outcome<S>> {
        if (isDisposed.get()) error("Dispatcher has been disposed")
        if (!isInitialized.get()) error("Dispatcher has not been initialized")
        return outcomeFlow.asSharedFlow()
    }

    private suspend fun processFeature(feature: @JvmSuppressWildcards Feature<S>) {
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
        if (remainingToSubscribe.decrementAndGet() == 0 && !subscribed.isCompleted) {
            subscribed.complete(Unit)
        }
    }

    fun shutdown() {
        if (isDisposed.getAndSet(true)) return
        
        featureJobs.forEach { job ->
            try {
                // Cancel the job properly using the Job's cancel method
                job.cancel()
            } catch (e: Exception) {
                // Log error but don't let it prevent other jobs from being cancelled
                println("Error cancelling feature job: ${e.message}")
            }
        }
    }

    fun isDisposed(): Boolean = isDisposed.get()
}