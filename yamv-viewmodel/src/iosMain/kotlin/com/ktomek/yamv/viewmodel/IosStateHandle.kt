package com.ktomek.yamv.viewmodel

import com.ktomek.yamv.state.StateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSUserDefaults

class IosStateHandle(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : StateHandle {
    private val flows = mutableMapOf<String, MutableStateFlow<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: String): T? = defaults.objectForKey(key) as? T

    override fun <T : Any> set(key: String, value: T) {
        defaults.setObject(value, key)
        @Suppress("UNCHECKED_CAST")
        (flows[key] as? MutableStateFlow<T>)?.value = value
    }

    override fun contains(key: String): Boolean = defaults.objectForKey(key) != null

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getStateFlow(key: String, initialValue: T): StateFlow<T> =
        (flows.getOrPut(key) { MutableStateFlow(get(key) ?: initialValue) } as MutableStateFlow<T>)
}
