package com.cola.pickly.di

import android.content.ContentResolver
import android.content.Context
import com.cola.pickly.data.analyzer.FaceDetectorHelper
import com.cola.pickly.data.analyzer.PhotoQualityAnalyzer
import com.cola.pickly.data.database.DatabaseModule
import com.cola.pickly.data.database.PhotoScoreDao
import com.cola.pickly.data.database.PicklyDatabase
import com.cola.pickly.data.photo.MediaStorePhotoRepository
import com.cola.pickly.domain.repository.PhotoRepository
import com.cola.pickly.domain.usecase.GroupPhotosByWeekUseCase
import com.cola.pickly.domain.usecase.PickBestPhotosUseCase
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
    fun provideGroupPhotosByWeekUseCase(): GroupPhotosByWeekUseCase {
        return GroupPhotosByWeekUseCase()
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
        return PickBestPhotosUseCase(analyzer, photoScoreDao)
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
}