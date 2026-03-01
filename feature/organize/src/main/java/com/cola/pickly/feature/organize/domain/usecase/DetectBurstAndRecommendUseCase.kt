package com.cola.pickly.feature.organize.domain.usecase

import com.cola.pickly.core.data.database.PhotoScoreDao
import com.cola.pickly.feature.organize.domain.engine.BurstDetectionEngine
import com.cola.pickly.core.model.BurstGroup
import com.cola.pickly.core.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 사진 목록에서 연사 그룹을 감지하고 베스트 사진을 추천하는 UseCase.
 *
 * DB에서 분석 점수를 로드하여 Photo에 반영한 후,
 * BurstDetectionEngine으로 연사 그룹을 감지합니다.
 */
class DetectBurstAndRecommendUseCase @Inject constructor(
    private val burstDetectionEngine: BurstDetectionEngine,
    private val photoScoreDao: PhotoScoreDao
) {
    /**
     * @param photos 분석할 사진 목록
     * @return 감지된 연사 그룹 목록
     */
    suspend operator fun invoke(photos: List<Photo>): BurstDetectionResult =
        withContext(Dispatchers.Default) {
            // DB에서 분석 점수 로드
            val scores = photoScoreDao.getScores(photos.map { it.id })

            // Photo 객체에 점수 반영
            val photosWithScores = photos.map { photo ->
                val entity = scores[photo.id]
                if (entity != null) {
                    photo.copy(recommendationScore = entity.score)
                } else {
                    photo
                }
            }

            val groups = burstDetectionEngine.detect(photosWithScores)

            val burstGroupByPhotoId = mutableMapOf<Long, BurstGroup>()
            groups.forEach { group ->
                group.photoIds.forEach { photoId ->
                    burstGroupByPhotoId[photoId] = group
                }
            }

            BurstDetectionResult(
                groups = groups,
                burstGroupByPhotoId = burstGroupByPhotoId
            )
        }
}

/**
 * 연사 감지 결과.
 *
 * @property groups 감지된 연사 그룹 목록
 * @property burstGroupByPhotoId 사진 ID → 해당 사진이 속한 그룹 매핑
 */
data class BurstDetectionResult(
    val groups: List<BurstGroup>,
    val burstGroupByPhotoId: Map<Long, BurstGroup>
)
