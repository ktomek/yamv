package com.ktomek.yamv.annotations

import com.ktomek.yamv.core.State
import kotlin.reflect.KClass

/**
 * Annotation to automatically generate state-related code.
 *
 * @property defaultState The default state class to be used if no other state is specified.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoState(val defaultState: KClass<out State> = NoDefaultState::class)

/**
 * Object representing the absence of a default state.
 */
object NoDefaultState : State
