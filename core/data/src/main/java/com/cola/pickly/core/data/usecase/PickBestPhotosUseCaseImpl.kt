package com.cola.pickly.core.data.usecase

import android.util.Log
import com.cola.pickly.core.data.analyzer.PhotoQualityAnalyzer
import com.cola.pickly.core.data.database.PhotoScoreDao
import com.cola.pickly.core.data.database.PhotoScoreEntity
import com.cola.pickly.core.domain.usecase.PickBestPhotosUseCase
import com.cola.pickly.core.model.Photo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * V2: 품질 점수 기반 추천 UseCase 구현체.
 * PhotoQualityAnalyzer를 사용하여 각 사진의 점수를 계산하고,
 * 점수가 높은 상위 2장을 추천 사진으로 선정합니다.
 *
 * DB 캐싱을 지원하여 이미 분석된 사진은 재분석하지 않습니다.
 */
class PickBestPhotosUseCaseImpl @Inject constructor(
    private val analyzer: PhotoQualityAnalyzer,
    private val photoScoreDao: PhotoScoreDao
) : PickBestPhotosUseCase {

    override suspend fun invoke(photos: List<Photo>): List<Photo> = coroutineScope {
        // 사진 단위 병렬 처리
        val deferredPhotos = photos.map { photo ->
            async {
                processPhoto(photo)
            }
        }
        deferredPhotos.awaitAll()
    }

    private suspend fun processPhoto(photo: Photo): Photo {
        // 캐싱된 점수가 있으면 DB에서 가져오고, 없으면 분석 후 DB에 저장
        val cachedScore = photoScoreDao.getScore(photo.id)
        return if (cachedScore != null) {
            Log.d("PickBestPhotos", "Cache hit for photo: ${photo.id}")
            photo.copy(recommendationScore = cachedScore.score)
        } else {
            Log.d("PickBestPhotos", "Analyzing photo: ${photo.id}")
            val score = analyzer.analyze(photo)
            // 분석 결과 DB 저장
            photoScoreDao.insertScore(
                PhotoScoreEntity(
                    photoId = photo.id,
                    score = score,
                    analyzedAt = System.currentTimeMillis()
                )
            )
            photo.copy(recommendationScore = score)
        }
    }
}
