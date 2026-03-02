package com.cola.pickly.feature.organize.domain.usecase

import com.cola.pickly.core.data.database.PhotoScoreDao
import com.cola.pickly.feature.organize.domain.engine.BurstDetectionEngine
import com.cola.pickly.feature.organize.domain.engine.RawBurstGroup
import com.cola.pickly.core.model.BurstGroup
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.RecommendationScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 사진 목록에서 연사 그룹을 감지하고 베스트 사진을 추천하는 UseCase.
 *
 * 2단계 분리:
 * - [detectGroups]: dHash 기반 그룹핑 (분석 점수 불필요, 병렬 실행 가능)
 * - [selectBestPhotos]: 점수 기반 베스트 선정
 * - [invoke]: 기존 호환용 1+2단계 통합
 */
class DetectBurstAndRecommendUseCase @Inject constructor(
    private val burstDetectionEngine: BurstDetectionEngine,
    private val photoScoreDao: PhotoScoreDao
) {
    /**
     * 1단계: dHash 기반 그룹 감지 (점수 불필요).
     * 분석과 병렬로 실행 가능.
     */
    suspend fun detectGroups(photos: List<Photo>): List<RawBurstGroup> {
        return burstDetectionEngine.detectGroups(photos)
    }

    /**
     * 2단계: 점수 기반 베스트 사진 선정.
     * @param rawGroups [detectGroups]의 결과
     * @param photos 점수를 가져올 사진 목록
     */
    suspend fun selectBestPhotos(
        rawGroups: List<RawBurstGroup>,
        photos: List<Photo>
    ): BurstDetectionResult = withContext(Dispatchers.Default) {
        val scores = photoScoreDao.getScores(photos.map { it.id })
        val scoreMap = photos.associate { photo ->
            photo.id to (scores[photo.id]?.score ?: RecommendationScore())
        }
        val groups = burstDetectionEngine.selectBestPhotos(rawGroups, scoreMap)
        toBurstDetectionResult(groups)
    }

    /**
     * 기존 호환용: 1+2단계 통합 실행.
     */
    suspend operator fun invoke(photos: List<Photo>): BurstDetectionResult =
        withContext(Dispatchers.Default) {
            val scores = photoScoreDao.getScores(photos.map { it.id })

            val photosWithScores = photos.map { photo ->
                val entity = scores[photo.id]
                if (entity != null) {
                    photo.copy(recommendationScore = entity.score)
                } else {
                    photo
                }
            }

            val groups = burstDetectionEngine.detect(photosWithScores)
            toBurstDetectionResult(groups)
        }

    private fun toBurstDetectionResult(groups: List<BurstGroup>): BurstDetectionResult {
        val burstGroupByPhotoId = mutableMapOf<Long, BurstGroup>()
        groups.forEach { group ->
            group.photoIds.forEach { photoId ->
                burstGroupByPhotoId[photoId] = group
            }
        }
        return BurstDetectionResult(
            groups = groups,
            burstGroupByPhotoId = burstGroupByPhotoId
        )
    }
}

/**
 * 연사 감지 결과.
 */
data class BurstDetectionResult(
    val groups: List<BurstGroup>,
    val burstGroupByPhotoId: Map<Long, BurstGroup>
)
