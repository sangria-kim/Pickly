package com.cola.pickly.di

import com.cola.pickly.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import javax.inject.Named

/**
 * 앱 수준의 DI 모듈.
 *
 * BuildConfig 값을 feature 모듈에 주입합니다.
 * feature 모듈은 app 모듈의 BuildConfig에 직접 접근할 수 없으므로 DI를 통해 주입합니다.
 */
@Module
@InstallIn(ViewModelComponent::class)
object AppModule {

    @Provides
    @Named("isDebugBuild")
    fun provideIsDebugBuild(): Boolean = BuildConfig.DEBUG

    @Provides
    @Named("appVersion")
    fun provideAppVersion(): String = BuildConfig.VERSION_NAME
}
