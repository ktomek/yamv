package com.ktomek.yamv.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface StateHandle {
    operator fun <T : Any> get(key: String): T?
    operator fun <T : Any> set(key: String, value: T)
    fun contains(key: String): Boolean
    fun <T : Any> getStateFlow(key: String, initialValue: T): StateFlow<T>
}

object EmptyStateHandle : StateHandle {
    override fun <T : Any> get(key: String): T? = null
    override fun <T : Any> set(key: String, value: T) = Unit
    override fun contains(key: String): Boolean = false
    override fun <T : Any> getStateFlow(key: String, initialValue: T): StateFlow<T> =
        MutableStateFlow(initialValue)
}
