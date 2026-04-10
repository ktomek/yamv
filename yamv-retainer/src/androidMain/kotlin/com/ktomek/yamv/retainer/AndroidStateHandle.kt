package com.ktomek.yamv.retainer

import androidx.lifecycle.SavedStateHandle
import com.ktomek.yamv.state.StateHandle
import kotlinx.coroutines.flow.StateFlow

class AndroidStateHandle(private val handle: SavedStateHandle) : StateHandle {
    override fun <T : Any> get(key: String): T? = handle[key]
    override fun <T : Any> set(key: String, value: T) { handle[key] = value }
    override fun contains(key: String): Boolean = handle.contains(key)
    override fun <T : Any> getStateFlow(key: String, initialValue: T): StateFlow<T> =
        handle.getStateFlow(key, initialValue)
}
