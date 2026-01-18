package com.cola.pickly.core.domain.usecase

import com.cola.pickly.core.model.Photo

/**
 * V2: 품질 점수 기반 추천 UseCase.
 * PhotoQualityAnalyzer를 사용하여 각 사진의 점수를 계산하고,
 * 점수가 높은 상위 2장을 추천 사진으로 선정합니다.
 *
 * DB 캐싱을 지원하여 이미 분석된 사진은 재분석하지 않습니다.
 * 
 * Note: PhotoQualityAnalyzer와 PhotoScoreDao는 data 모듈에 있으며,
 * 이 UseCase는 data 모듈에서 구현됩니다.
 */
interface PickBestPhotosUseCase {
    suspend operator fun invoke(photos: List<Photo>): List<Photo>
}
