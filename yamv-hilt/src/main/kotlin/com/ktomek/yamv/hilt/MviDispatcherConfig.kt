package com.ktomek.yamv.hilt

import com.ktomek.yamv.core.State
import javax.inject.Qualifier
import kotlin.reflect.KClass

/**
 * Dagger qualifier for providing a per-state [com.ktomek.yamv.state.CoroutineDispatcherConfig].
 *
 * The KSP processor references this annotation in generated `*Store` constructors and
 * `*FeaturesModule` optional-binding declarations. Provide a binding with this qualifier to
 * override the default dispatcher configuration for a specific state type.
 *
 * Usage:
 * ```kotlin
 * @Module
 * @InstallIn(ViewModelComponent::class)
 * object CounterDispatcherModule {
 *     @Provides @MviDispatcherConfig(CounterState::class)
 *     fun provide(): CoroutineDispatcherConfig = MyCounterConfig()
 * }
 * ```
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class MviDispatcherConfig(val value: KClass<out State>)
