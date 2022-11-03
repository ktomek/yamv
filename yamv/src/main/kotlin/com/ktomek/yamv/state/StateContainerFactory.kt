package com.ktomek.yamv.state

import com.ktomek.yamv.core.EffectOutcome
import com.ktomek.yamv.core.Outcome
import com.ktomek.yamv.core.State
import kotlinx.coroutines.CoroutineScope

interface StateContainerFactory<S : State, R : Outcome<S>, E : EffectOutcome<S>> {
    fun create(scope: CoroutineScope): StateContainer<S, R, E>
}
