package com.ktomek.yamv.hilt

import com.ktomek.yamv.state.CoroutineDispatcherConfig
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Declares an optional unqualified [CoroutineDispatcherConfig] binding.
 *
 * This enables the generated `*Store` classes to accept an `Optional<CoroutineDispatcherConfig>`
 * as a global default, which is overridden by per-state bindings qualified with
 * [MviDispatcherConfig].
 *
 * Provide a binding via [@AutoDispatcherConfig][com.ktomek.yamv.annotations.AutoDispatcherConfig]
 * (no arguments) or manually with an unqualified `@Provides`.
 */
@Module
@InstallIn(ViewModelComponent::class)
interface YamvDispatcherModule {
    @BindsOptionalOf
    fun bindDefaultDispatcherConfig(): CoroutineDispatcherConfig
}
