package com.ktomek.yamv.intention

import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.feature.FeatureFlow
import com.ktomek.yamv.feature.featureScope
import com.ktomek.yamv.state.DefaultStore
import com.ktomek.yamv.state.Store
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import java.io.Closeable
import javax.inject.Inject

/**
 * Base MVI dispatcher which is taking intentions and sending them to actions.
 */
class DefaultIntentionDispatcher<OUTCOME>
@Inject constructor(
    private val flowFeatures: Set<@JvmSuppressWildcards FeatureFlow<OUTCOME>>,
    private val features: Set<@JvmSuppressWildcards Feature<OUTCOME>>,
    store: DefaultStore
) : IntentionDispatcher<OUTCOME> {

    private val resultFlow = MutableSharedFlow<OUTCOME>()
    private val intentionFlows = mutableListOf<Pair<FeatureFlow<OUTCOME>, MutableSharedFlow<Any>>>()

    init {
        flowFeatures
            .toMutableList()
            .apply { addAll(features.map { it.wrap() }) }
            .forEach { featureFlow ->
                val intentions = MutableSharedFlow<Any>()
                with(featureFlow) {
                    dispatcher?.let { dispatcher ->
                        featureScope.launch(dispatcher) {
                            featureFlow(intentions, store)
                                .flowOn(dispatcher)
                                .collect(resultFlow::emit)
                        }
                    } ?: featureScope.launch {
                        featureFlow(intentions, store)
                            .collect(resultFlow::emit)
                    }
                }
                intentionFlows.add(featureFlow to intentions)
            }
    }

    override suspend fun dispatchIntention(intention: Any) {
        intentionFlows.forEach { (f, i) ->
            f.dispatcher?.let { dispatcher ->
                f.featureScope.launch(dispatcher) {
                    i.emit(intention)
                }
            } ?: f.featureScope.launch { i.emit(intention) }
        }
    }

    override fun close() {
        flowFeatures.forEach(Closeable::close)
        features.forEach(Closeable::close)
    }

    override fun observeResults(): SharedFlow<OUTCOME> = resultFlow.asSharedFlow()

    private fun Feature<OUTCOME>.wrap(): FeatureFlow<OUTCOME> =
        object : FeatureFlow<OUTCOME>() {
            override suspend operator fun invoke(intentions: Flow<Any>, store: Store): Flow<OUTCOME> =
                channelFlow {
                    this@wrap.dispatcher?.invoke {
                        intentions
                            .map { intention -> this@wrap.invoke(intention, store) }
                            .collect(::send)
                    } ?: intentions
                        .map { intention -> this@wrap.invoke(intention, store) }
                        .collect(::send)
                }

            override fun close() {
                super.close()
                this@wrap.close()
            }
        }
}
