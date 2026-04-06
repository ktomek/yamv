package com.ktomek.yamv.state

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface CoroutineDispatcherConfig {
    fun provideIntentionDispatcher(intention: Any?): CoroutineDispatcher
    fun provideReducerDispatcher(): CoroutineDispatcher
    fun provideFeatureDispatcher(feature: Any): CoroutineDispatcher
}

class DefaultCoroutineDispatcherConfig(
    private val default: CoroutineDispatcher = Dispatchers.Default,
    private val ui: CoroutineDispatcher = Dispatchers.Main,
) : CoroutineDispatcherConfig {
    override fun provideIntentionDispatcher(intention: Any?): CoroutineDispatcher = ui
    override fun provideReducerDispatcher(): CoroutineDispatcher = ui
    override fun provideFeatureDispatcher(feature: Any): CoroutineDispatcher = default
}
