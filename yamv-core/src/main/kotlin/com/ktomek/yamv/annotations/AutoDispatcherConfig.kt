package com.ktomek.yamv.annotations

import com.ktomek.yamv.core.State
import kotlin.reflect.KClass

/**
 * Annotate a [com.ktomek.yamv.state.CoroutineDispatcherConfig] implementation to have the KSP
 * processor generate a Dagger module that provides it automatically.
 *
 * - **No arguments** — global default for all states without a specific override
 * - **One state** — per-state override
 * - **Multiple states** — same config for several states
 *
 * Precedence: per-state > global default > [com.ktomek.yamv.state.DefaultCoroutineDispatcherConfig]
 *
 * ```kotlin
 * @AutoDispatcherConfig
 * class IoDispatcherConfig : CoroutineDispatcherConfig { ... }
 *
 * @AutoDispatcherConfig(CounterState::class)
 * class CounterDispatcherConfig : CoroutineDispatcherConfig { ... }
 *
 * @AutoDispatcherConfig(TimerState::class, AnimationState::class)
 * class SharedConfig : CoroutineDispatcherConfig { ... }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoDispatcherConfig(vararg val value: KClass<out State>)
