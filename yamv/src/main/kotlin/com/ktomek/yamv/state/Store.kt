package com.ktomek.yamv.state

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface Store {
    fun <S : State> observeStates(stateType: Class<S>): StateFlow<S>
}

interface StateStore<S : State, E : EffectOutcome<S>> {
    val state: StateFlow<S>
    val effects: Flow<E>
    fun dispatch(intention: Any)
    operator fun invoke(intention: Any)
}

internal interface InternalStore : Store {
    fun register(stateContainer: StateContainer<*, *, *>)
    fun unregister(stateContainer: StateContainer<*, *, *>)
    operator fun invoke(intention: Any)
    fun dispatch(intention: Any)
}

@Singleton
class DefaultStore @Inject constructor() : InternalStore {

    private val mvs = mutableMapOf<Class<out State>, StateContainer<*, *, *>>()

    override fun register(stateContainer: StateContainer<*, *, *>) {
        mvs[stateContainer.stateType] = stateContainer
    }

    override fun unregister(stateContainer: StateContainer<*, *, *>) {
        mvs.remove(stateContainer.stateType)
    }

    override fun invoke(intention: Any) {
        mvs.forEach { it.value.dispatchIntention(intention) }
    }

    override fun dispatch(intention: Any) {
        invoke(intention)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <S : State> observeStates(stateType: Class<S>): StateFlow<S> {
        val stateStore = mvs[stateType]
        check(stateStore != null) { "No state container for $stateType" }
        return stateStore.state as StateFlow<S>
    }
}
