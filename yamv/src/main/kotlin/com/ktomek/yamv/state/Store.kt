package com.ktomek.yamv.state

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface representing a store that can observe states.
 */
interface Store {
    /**
     * Observes states of a given type.
     *
     * @param S The type of the state.
     * @param stateType The class of the state type.
     * @return A [StateFlow] emitting the states of the given type.
     */
    fun <S : State> observeStates(stateType: Class<S>): StateFlow<S>
}

/**
 * Interface representing a state store that can dispatch intentions and observe states and effects.
 *
 * @param S The type of the state.
 * @param E The type of the effect outcome.
 */
interface StateStore<S : State, E : EffectOutcome<S>> {
    /**
     * The current state as a [StateFlow].
     */
    val state: StateFlow<S>

    /**
     * The effects as a [Flow].
     */
    val effects: Flow<E>

    /**
     * Dispatches an intention.
     *
     * @param intention The intention to be dispatched.
     */
    fun dispatch(intention: Any)

    /**
     * Invokes the store with an intention.
     *
     * @param intention The intention to be invoked.
     */
    operator fun invoke(intention: Any)
}

/**
 * Internal interface representing a store with additional registration and unregistration capabilities.
 */
internal interface InternalStore : Store {
    /**
     * Registers a state container.
     *
     * @param stateContainer The state container to be registered.
     */
    fun register(stateContainer: StateContainer<*, *, *>)

    /**
     * Unregisters a state container.
     *
     * @param stateContainer The state container to be unregistered.
     */
    fun unregister(stateContainer: StateContainer<*, *, *>)

    /**
     * Invokes the store with an intention.
     *
     * @param intention The intention to be invoked.
     */
    operator fun invoke(intention: Any)

    /**
     * Dispatches an intention.
     *
     * @param intention The intention to be dispatched.
     */
    fun dispatch(intention: Any)
}

/**
 * Default implementation of [InternalStore].
 *
 * @constructor Creates an instance of [DefaultStore].
 */
@Singleton
class DefaultStore @Inject constructor() : InternalStore {

    private val mvs = mutableMapOf<Class<out State>, StateContainer<*, *, *>>()

    /**
     * Registers a state container.
     *
     * @param stateContainer The state container to be registered.
     */
    override fun register(stateContainer: StateContainer<*, *, *>) {
        mvs[stateContainer.stateType] = stateContainer
    }

    /**
     * Unregisters a state container.
     *
     * @param stateContainer The state container to be unregistered.
     */
    override fun unregister(stateContainer: StateContainer<*, *, *>) {
        mvs.remove(stateContainer.stateType)
    }

    /**
     * Invokes the store with an intention.
     *
     * @param intention The intention to be invoked.
     */
    override fun invoke(intention: Any) {
        mvs.forEach { it.value.dispatchIntention(intention) }
    }

    /**
     * Dispatches an intention.
     *
     * @param intention The intention to be dispatched.
     */
    override fun dispatch(intention: Any) {
        invoke(intention)
    }

    /**
     * Observes states of a given type.
     *
     * @param S The type of the state.
     * @param stateType The class of the state type.
     * @return A [StateFlow] emitting the states of the given type.
     * @throws IllegalStateException if no state container is found for the given type.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <S : State> observeStates(stateType: Class<S>): StateFlow<S> {
        val stateStore = mvs[stateType]
        check(stateStore != null) { "No state container for $stateType" }
        return stateStore.state as StateFlow<S>
    }
}
