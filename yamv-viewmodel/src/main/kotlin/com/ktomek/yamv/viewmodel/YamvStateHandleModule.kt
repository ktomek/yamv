package com.ktomek.yamv.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.ktomek.yamv.state.StateHandle
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object YamvStateHandleModule {
    @Provides
    @ViewModelScoped
    fun provideStateHandle(savedStateHandle: SavedStateHandle): StateHandle =
        AndroidStateHandle(savedStateHandle)
}
