package com.cola.pickly.feature.organize.domain.usecase

import android.util.Log
import com.cola.pickly.core.data.analyzer.AnalysisCriteriaHasher
import com.cola.pickly.core.data.analyzer.PhotoQualityAnalyzerFactory
import com.cola.pickly.core.data.database.PhotoScoreDao
import com.cola.pickly.core.data.database.PhotoScoreEntity
import com.cola.pickly.core.data.settings.SettingsRepository
import com.cola.pickly.core.domain.repository.PhotoRepository
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
 * 분석 기준이 동일하고 파일이 수정되지 않은 경우 캐시를 재사용합니다.
 */
class AnalyzePhotosForAutoRejectUseCase @Inject constructor(
    private val analyzerFactory: PhotoQualityAnalyzerFactory,
    private val settingsRepository: SettingsRepository,
    private val photoScoreDao: PhotoScoreDao,
    private val photoRepository: PhotoRepository
) {
    /**
     * 사진 목록을 분석하여 제외 후보 ID 목록을 반환합니다.
     *
     * @param photos 분석할 사진 목록
     * @return 분석 결과 (제외 후보 ID, 분석된 사진 수, 캐시 히트 수, 소요 시간)
     */
    suspend operator fun invoke(photos: List<Photo>): AnalysisResult = withContext(Dispatchers.Default) {
        if (photos.isEmpty()) {
            return@withContext AnalysisResult(
                candidates = emptyMap(),
                totalAnalyzed = 0,
                cacheHits = 0,
                analysisDurationMs = 0L
            )
        }

        // 1. 현재 분석 기준
        val currentSettings = settingsRepository.settings.first()
        val currentHash = AnalysisCriteriaHasher.computeHash(
            criteria = currentSettings.smartDiscardCriteria,
            thresholds = currentSettings.smartDiscardThresholds
        )

        // 2. 배치 조회
        val photoModifiedTimes = photoRepository.getModifiedTimes(photos.map { it.id })
        val cachedScores = photoScoreDao.getScores(photos.map { it.id })

        var cacheHits = 0
        var candidates: Map<Long, RejectReason> = emptyMap()

        val analysisDurationMs = measureTimeMillis {
            // 3. 캐시 검증 및 분류
            val (validCached, needsReanalysis) = photos.partition { photo ->
                val cached = cachedScores[photo.id]
                val modifiedAt = photoModifiedTimes[photo.id] ?: 0L

                cached != null && isCacheValid(
                    cached,
                    currentHash,
                    modifiedAt,
                    PhotoScoreEntity.CURRENT_SCHEMA_VERSION
                )
            }

            cacheHits = validCached.size
            Log.d(TAG, "Cache: ${cacheHits}/${photos.size} hits")

            // 4. 캐시 재사용
            val cachedCandidates = validCached.mapNotNull { photo ->
                val score = cachedScores[photo.id]!!.score
                if (score.isCutoff && score.rejectReason != null) {
                    photo.id to score.rejectReason!!
                } else null
            }.toMap()

            // 5. 재분석 (병렬 처리)
            val analyzer = analyzerFactory.create(
                currentSettings.smartDiscardThresholds,
                currentSettings.smartDiscardCriteria
            )

            val reanalyzedCandidates = needsReanalysis
                .chunked(4)
                .flatMap { chunk ->
                    chunk.map { photo ->
                        async {
                            try {
                                val score = analyzer.analyze(photo)
                                val modifiedAt = photoModifiedTimes[photo.id]
                                    ?: System.currentTimeMillis()

                                photoScoreDao.insertScore(
                                    PhotoScoreEntity(
                                        photoId = photo.id,
                                        score = score,
                                        analyzedAt = System.currentTimeMillis(),
                                        analysisCriteriaHash = currentHash,
                                        modifiedAt = modifiedAt,
                                        schemaVersion = PhotoScoreEntity.CURRENT_SCHEMA_VERSION
                                    )
                                )

                                if (score.isCutoff && score.rejectReason != null) {
                                    photo.id to score.rejectReason!!
                                } else null
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }.awaitAll()
                }
                .filterNotNull()
                .toMap()

            candidates = cachedCandidates + reanalyzedCandidates
        }

        AnalysisResult(
            candidates = candidates,
            totalAnalyzed = photos.size,
            cacheHits = cacheHits,
            analysisDurationMs = analysisDurationMs
        )
    }

    private fun isCacheValid(
        cached: PhotoScoreEntity,
        currentHash: String,
        currentModifiedAt: Long,
        currentSchemaVersion: Int
    ): Boolean {
        return cached.schemaVersion == currentSchemaVersion &&
               cached.analysisCriteriaHash == currentHash &&
               cached.modifiedAt == currentModifiedAt
    }

    companion object {
        private const val TAG = "AnalyzePhotosForAutoReject"
    }
}

/**
 * 분석 결과 데이터 클래스.
 *
 * @property candidates 제외 후보 사진 ID와 제외 사유 매핑
 * @property totalAnalyzed 분석된 전체 사진 수
 * @property cacheHits 캐시에서 재사용된 사진 수
 * @property analysisDurationMs 분석 소요 시간 (밀리초)
 */
data class AnalysisResult(
    val candidates: Map<Long, RejectReason>,
    val totalAnalyzed: Int,
    val cacheHits: Int,
    val analysisDurationMs: Long
)
