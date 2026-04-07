package com.ktomek.yamv.state

import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

interface MviRegistry {
    fun <S : State> observeStates(stateType: KClass<S>): StateFlow<S>
}

internal interface MutableMviRegistry : MviRegistry {
    fun register(mviRuntime: MviRuntime<*>)
    fun unregister(mviRuntime: MviRuntime<*>)
    operator fun invoke(intention: Any)
    fun dispatch(intention: Any)
}

internal class DefaultMviRegistry : MutableMviRegistry {

    private val runtimes = mutableMapOf<KClass<out State>, MviRuntime<*>>()

    override fun register(mviRuntime: MviRuntime<*>) {
        runtimes[mviRuntime.defaultState::class] = mviRuntime
    }

    override fun unregister(mviRuntime: MviRuntime<*>) {
        runtimes.remove(mviRuntime.defaultState::class)
    }

    override fun invoke(intention: Any) {
        runtimes.forEach { it.value.dispatch(intention) }
    }

    override fun dispatch(intention: Any) {
        invoke(intention)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <S : State> observeStates(stateType: KClass<S>): StateFlow<S> {
        val runtime = runtimes[stateType]
        check(runtime != null) { "No MviRuntime for $stateType" }
        return runtime.state as StateFlow<S>
    }
}
