package com.ktomek.yamv.counter

import com.ktomek.yamv.feature.Feature
import com.ktomek.yamv.ui.counter.logic.feature.AutoDecreaseFeature
import com.ktomek.yamv.ui.counter.logic.feature.AutoIncreaseFeature
import com.ktomek.yamv.ui.counter.logic.feature.DecreaseFeature
import com.ktomek.yamv.ui.counter.logic.feature.increaseFeature
import com.ktomek.yamv.ui.counter.logic.state.CounterState
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.multibindings.IntoSet

@Module
@InstallIn(ViewModelComponent::class)
interface CounterFeaturesModule {

    @Binds @IntoSet @ViewModelScoped
    fun bindDecreaseFeature(feature: DecreaseFeature): Feature<CounterState>

    @Binds @IntoSet @ViewModelScoped
    fun bindAutoIncreaseFeature(feature: AutoIncreaseFeature): Feature<CounterState>

    @Binds @IntoSet @ViewModelScoped
    fun bindAutoDecreaseFeature(feature: AutoDecreaseFeature): Feature<CounterState>

    companion object {
        @Provides @IntoSet @ViewModelScoped
        fun provideIncreaseFeature(): Feature<CounterState> = increaseFeature
    }
}
