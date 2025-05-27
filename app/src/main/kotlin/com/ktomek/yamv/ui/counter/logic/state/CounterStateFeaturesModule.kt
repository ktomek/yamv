package com.ktomek.yamv.ui.counter.logic.state

import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.ui.counter.logic.feature.increaseFeature
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.multibindings.IntoSet

@Module
@InstallIn(ViewModelComponent::class)
interface CounterStateModule {

    companion object {

        @Provides
        @IntoSet
        @ViewModelScoped
        fun providesIncreaseFeature(): Feature<CounterState> = increaseFeature
    }
}
