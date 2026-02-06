package com.cola.pickly.feature.organize.domain.usecase

import com.cola.pickly.core.data.analyzer.PhotoQualityAnalyzerFactory
import com.cola.pickly.core.data.database.PhotoScoreDao
import com.cola.pickly.core.data.database.PhotoScoreEntity
import com.cola.pickly.core.data.settings.SettingsRepository
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.RejectReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.system.measureTimeMillis

/**
 * 사진 목록을 분석하여 자동 제외 후보를 식별하는 UseCase.
 * PhotoQualityAnalyzer를 사용하여 품질 기준 미달 사진을 찾습니다.
 *
 * 사용자 설정(SmartDiscardThresholds)을 실시간으로 적용하여 분석합니다.
 * 분석 결과는 DB에 저장되어 캐시로 활용됩니다.
 */
class AnalyzePhotosForAutoRejectUseCase @Inject constructor(
    private val analyzerFactory: PhotoQualityAnalyzerFactory,
    private val settingsRepository: SettingsRepository,
    private val photoScoreDao: PhotoScoreDao
) {
    /**
     * 사진 목록을 분석하여 제외 후보 ID 목록을 반환합니다.
     *
     * @param photos 분석할 사진 목록
     * @return 분석 결과 (제외 후보 ID, 분석된 사진 수, 소요 시간)
     */
    suspend operator fun invoke(photos: List<Photo>): AnalysisResult = withContext(Dispatchers.Default) {
        if (photos.isEmpty()) {
            return@withContext AnalysisResult(
                candidates = emptyMap(),
                totalAnalyzed = 0,
                analysisDurationMs = 0L
            )
        }

        // 최신 설정 가져오기 (분석 시작 전 1회)
        val currentSettings = settingsRepository.settings.first()
        val analyzer = analyzerFactory.create(currentSettings.smartDiscardThresholds, currentSettings.smartDiscardCriteria)

        var candidates: Map<Long, RejectReason> = emptyMap()
        val analysisDurationMs = measureTimeMillis {
            candidates = photos
                .chunked(4) // 병렬 처리 최적화 (4개씩)
                .flatMap { chunk ->
                    chunk.map { photo ->
                        async {
                            try {
                                val score = analyzer.analyze(photo)

                                // 분석 결과를 DB에 저장 (캐시로 활용)
                                photoScoreDao.insertScore(
                                    PhotoScoreEntity(
                                        photoId = photo.id,
                                        score = score,
                                        analyzedAt = System.currentTimeMillis()
                                    )
                                )

                                if (score.isCutoff && score.rejectReason != null) {
                                    photo.id to score.rejectReason!!
                                } else null
                            } catch (e: Exception) {
                                // 분석 실패 시 제외 후보로 포함하지 않음
                                null
                            }
                        }
                    }.awaitAll()
                }
                .filterNotNull()
                .toMap()
        }

        AnalysisResult(
            candidates = candidates,
            totalAnalyzed = photos.size,
            analysisDurationMs = analysisDurationMs
        )
    }
}

/**
 * 분석 결과 데이터 클래스.
 *
 * @property candidates 제외 후보 사진 ID와 제외 사유 매핑
 * @property totalAnalyzed 분석된 전체 사진 수
 * @property analysisDurationMs 분석 소요 시간 (밀리초)
 */
data class AnalysisResult(
    val candidates: Map<Long, RejectReason>,
    val totalAnalyzed: Int,
    val analysisDurationMs: Long
)
