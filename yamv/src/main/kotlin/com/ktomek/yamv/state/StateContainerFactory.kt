package com.ktomek.yamv.state

import com.ktomek.yamv.core.State
import kotlinx.coroutines.CoroutineScope

interface StateContainerFactory<S : State> {
    fun create(scope: CoroutineScope): StateContainer<S>
}
