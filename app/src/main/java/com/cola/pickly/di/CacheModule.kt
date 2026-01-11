package com.cola.pickly.di

import com.cola.pickly.data.cache.CacheRepository
import com.cola.pickly.data.cache.DefaultCacheRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CacheModule {

    @Binds
    @Singleton
    abstract fun bindCacheRepository(
        impl: DefaultCacheRepository
    ): CacheRepository
}


