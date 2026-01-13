package com.cola.pickly.core.data.di

import com.cola.pickly.core.data.refresh.InMemoryPhotoDataRefreshNotifier
import com.cola.pickly.core.domain.refresh.PhotoDataRefreshNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RefreshNotifierModule {
    @Binds
    abstract fun bindPhotoDataRefreshNotifier(
        impl: InMemoryPhotoDataRefreshNotifier
    ): PhotoDataRefreshNotifier
}

