package com.ktomek.yamv.feature

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.Closeable
import java.io.IOException
import kotlin.coroutines.CoroutineContext

private const val JOB_KEY = "androidx.lifecycle.ViewModelCoroutineScope.JOB_KEY"
internal val IFeature.internalFeatureScope: CoroutineScope
    get() {
        val scope: CoroutineScope? = this.getTag(JOB_KEY)
        if (scope != null) {
            return scope
        }
        return setTagIfAbsent(
            JOB_KEY,
            CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        )
    }

internal class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {
    override val coroutineContext: CoroutineContext = context

    override fun close() {
        coroutineContext.cancel()
    }
}

internal interface IFeature : Closeable {
    fun <T : Any> setTagIfAbsent(key: String?, newValue: T): T

    fun <T> getTag(key: String?): T?
}

internal class DefaultFeature : IFeature {
    private val bagOfTags: MutableMap<String?, Any> = mutableMapOf()
    private val closeables: MutableSet<Closeable> = mutableSetOf()
    private var cleared = false

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> setTagIfAbsent(key: String?, newValue: T): T {
        var previous: T?
        synchronized(bagOfTags) {
            previous = bagOfTags[key] as? T?
            if (previous == null) {
                bagOfTags[key] = newValue
            }
        }
        val result = if (previous == null) newValue else previous!!
        if (cleared) {
            closeWithRuntimeException(result)
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getTag(key: String?): T? {
        synchronized(bagOfTags) { return bagOfTags[key] as T? }
    }

    override fun close() {
        cleared = true
        synchronized(bagOfTags) {
            bagOfTags.values.forEach(Companion::closeWithRuntimeException)
        }
        synchronized(closeables) {
            closeables.forEach(Companion::closeWithRuntimeException)
        }
    }

    @Suppress("TooGenericExceptionThrown")
    companion object {
        private fun closeWithRuntimeException(obj: Any) {
            if (obj is Closeable) {
                try {
                    obj.close()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}
