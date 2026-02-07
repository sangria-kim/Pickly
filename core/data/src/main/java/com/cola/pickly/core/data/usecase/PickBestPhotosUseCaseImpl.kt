package com.cola.pickly.core.data.usecase

import android.util.Log
import com.cola.pickly.core.data.analyzer.AnalysisCriteriaHasher
import com.cola.pickly.core.data.analyzer.PhotoQualityAnalyzer
import com.cola.pickly.core.data.analyzer.PhotoQualityAnalyzerFactory
import com.cola.pickly.core.data.database.PhotoScoreDao
import com.cola.pickly.core.data.database.PhotoScoreEntity
import com.cola.pickly.core.data.settings.SettingsRepository
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.domain.usecase.PickBestPhotosUseCase
import com.cola.pickly.core.model.Photo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * V2: 품질 점수 기반 추천 UseCase 구현체.
 * PhotoQualityAnalyzer를 사용하여 각 사진의 점수를 계산하고,
 * 점수가 높은 상위 2장을 추천 사진으로 선정합니다.
 *
 * DB 캐싱을 지원하여 이미 분석된 사진은 재분석하지 않습니다.
 * 분석 기준이 동일하고 파일이 수정되지 않은 경우 캐시를 재사용합니다.
 */
class PickBestPhotosUseCaseImpl @Inject constructor(
    private val analyzerFactory: PhotoQualityAnalyzerFactory,
    private val settingsRepository: SettingsRepository,
    private val photoScoreDao: PhotoScoreDao,
    private val photoRepository: PhotoRepository
) : PickBestPhotosUseCase {

    override suspend fun invoke(photos: List<Photo>): List<Photo> = coroutineScope {
        val currentSettings = settingsRepository.settings.first()
        val analyzer = analyzerFactory.create(currentSettings.smartDiscardThresholds)

        // 현재 분석 기준 해시 계산
        val currentHash = AnalysisCriteriaHasher.computeHash(
            criteria = currentSettings.smartDiscardCriteria,
            thresholds = currentSettings.smartDiscardThresholds
        )

        // 배치로 수정 시각 및 캐시 조회
        val photoModifiedTimes = photoRepository.getModifiedTimes(photos.map { it.id })
        val cachedScores = photoScoreDao.getScores(photos.map { it.id })

        // 사진 단위 병렬 처리
        val deferredPhotos = photos.map { photo ->
            async {
                processPhoto(
                    photo,
                    analyzer,
                    currentHash,
                    cachedScores[photo.id],
                    photoModifiedTimes[photo.id] ?: System.currentTimeMillis()
                )
            }
        }
        deferredPhotos.awaitAll()
    }

    private suspend fun processPhoto(
        photo: Photo,
        analyzer: PhotoQualityAnalyzer,
        currentHash: String,
        cachedScore: PhotoScoreEntity?,
        currentModifiedAt: Long
    ): Photo {
        // 캐시 유효성 검증
        val isCacheValid = cachedScore != null &&
                cachedScore.schemaVersion == PhotoScoreEntity.CURRENT_SCHEMA_VERSION &&
                cachedScore.analysisCriteriaHash == currentHash &&
                cachedScore.modifiedAt == currentModifiedAt

        return if (isCacheValid) {
            Log.d(TAG, "Cache hit for photo: ${photo.id}")
            photo.copy(recommendationScore = cachedScore!!.score)
        } else {
            Log.d(TAG, "Analyzing photo: ${photo.id}")
            val score = analyzer.analyze(photo)
            // 분석 결과 DB 저장
            photoScoreDao.insertScore(
                PhotoScoreEntity(
                    photoId = photo.id,
                    score = score,
                    analyzedAt = System.currentTimeMillis(),
                    analysisCriteriaHash = currentHash,
                    modifiedAt = currentModifiedAt,
                    schemaVersion = PhotoScoreEntity.CURRENT_SCHEMA_VERSION
                )
            )
            photo.copy(recommendationScore = score)
        }
    }

    companion object {
        private const val TAG = "PickBestPhotos"
    }
}
