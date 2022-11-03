package com.ktomek.yamv.annotations

import com.ktomek.yamv.core.State
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoState(val defaultState: KClass<out State> = NoDefaultState::class)

object NoDefaultState : State
