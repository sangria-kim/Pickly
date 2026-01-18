package com.cola.pickly.core.data.di

import android.content.ContentResolver
import android.content.Context
import com.cola.pickly.core.data.analyzer.FaceDetectorHelper
import com.cola.pickly.core.data.analyzer.PhotoQualityAnalyzer
import com.cola.pickly.core.data.database.DatabaseModule
import com.cola.pickly.core.data.database.PhotoScoreDao
import com.cola.pickly.core.data.database.PicklyDatabase
import com.cola.pickly.core.data.photo.MediaStorePhotoRepository
import com.cola.pickly.core.data.photo.MediaStorePhotoActionRepository
import com.cola.pickly.core.data.photo.PhotoActionRepository
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.data.usecase.PickBestPhotosUseCaseImpl

import com.cola.pickly.core.domain.usecase.PickBestPhotosUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideFaceDetectorHelper(): FaceDetectorHelper {
        return FaceDetectorHelper()
    }

    @Provides
    @Singleton
    fun providePhotoQualityAnalyzer(faceDetectorHelper: FaceDetectorHelper): PhotoQualityAnalyzer {
        return PhotoQualityAnalyzer(faceDetectorHelper)
    }



    @Provides
    @Singleton
    fun providePicklyDatabase(@ApplicationContext context: Context): PicklyDatabase {
        return DatabaseModule.getDatabase(context)
    }

    @Provides
    @Singleton
    fun providePhotoScoreDao(database: PicklyDatabase): PhotoScoreDao {
        return database.photoScoreDao()
    }

    @Provides
    @Singleton
    fun providePickBestPhotosUseCase(
        analyzer: PhotoQualityAnalyzer,
        photoScoreDao: PhotoScoreDao
    ): PickBestPhotosUseCase {
        return PickBestPhotosUseCaseImpl(analyzer, photoScoreDao)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(
        mediaStorePhotoRepository: MediaStorePhotoRepository
    ): PhotoRepository

    @Binds
    @Singleton
    abstract fun bindPhotoActionRepository(
        mediaStorePhotoActionRepository: MediaStorePhotoActionRepository
    ): PhotoActionRepository
}