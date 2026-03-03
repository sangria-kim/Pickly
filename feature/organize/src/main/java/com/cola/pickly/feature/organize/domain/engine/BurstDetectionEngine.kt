package com.cola.pickly.feature.organize.domain.engine

import android.graphics.Bitmap
import android.util.Log
import com.cola.photoanalyzer.internal.BitmapLoader
import com.cola.pickly.core.model.BurstGroup
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.RecommendationScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 연속 촬영(버스트) 사진 그룹을 감지하는 엔진.
 *
 * 2단계 분리:
 * - [detectGroups]: dHash 기반 그룹핑 (점수 불필요)
 * - [selectBestPhotos]: 점수 기반 베스트 선정
 */
@Singleton
class BurstDetectionEngine @Inject constructor() {

    /**
     * 1단계: dHash 기반 그룹 감지 (점수 불필요).
     * @return Raw 버스트 그룹 (photoIds만 포함, bestPhotoId 미설정)
     */
    suspend fun detectGroups(photos: List<Photo>): List<RawBurstGroup> = withContext(Dispatchers.Default) {
        if (photos.size < 2) return@withContext emptyList()

        val sorted = photos.sortedBy { it.takenAt }

        // dHash 병렬 계산
        val hashes = sorted.map { photo ->
            async { computeDHash(photo.filePath) }
        }.awaitAll()

        // 인접 사진 체이닝
        val groups = mutableListOf<MutableList<Photo>>()
        var currentGroup = mutableListOf(sorted.first())

        for (i in 1 until sorted.size) {
            val timeDiff = sorted[i].takenAt - sorted[i - 1].takenAt
            val hash1 = hashes[i - 1]
            val hash2 = hashes[i]

            val threshold = when {
                timeDiff !in 0..CHAIN_TIME_MS -> -1
                timeDiff <= FAST_BURST_TIME_MS -> DHASH_THRESHOLD_RELAXED
                else -> DHASH_THRESHOLD
            }
            val shouldChain = threshold >= 0 && (
                hash1 == null || hash2 == null ||
                    hammingDistance(hash1, hash2) <= threshold
                )

            Log.d(TAG, "Photo[${i - 1}]->[${i}]: timeDiff=${timeDiff}ms, " +
                "hamming=${if (hash1 != null && hash2 != null) hammingDistance(hash1, hash2) else -1}, " +
                "chained=$shouldChain")

            if (shouldChain) {
                currentGroup.add(sorted[i])
            } else {
                if (currentGroup.size >= MIN_GROUP_SIZE) groups.add(currentGroup)
                currentGroup = mutableListOf(sorted[i])
            }
        }
        if (currentGroup.size >= MIN_GROUP_SIZE) groups.add(currentGroup)

        groups.mapIndexed { index, groupPhotos ->
            RawBurstGroup(
                groupIndex = index,
                photoIds = groupPhotos.map { it.id }
            )
        }
    }

    /**
     * 2단계: 점수 기반 베스트 사진 선정.
     * @param rawGroups [detectGroups]의 결과
     * @param scoreMap photoId → RecommendationScore 매핑
     */
    fun selectBestPhotos(
        rawGroups: List<RawBurstGroup>,
        scoreMap: Map<Long, RecommendationScore>
    ): List<BurstGroup> {
        return rawGroups.map { raw ->
            val scoreComparator = compareByDescending<Long> {
                scoreMap[it]?.totalScore ?: 0.0
            }.thenByDescending {
                scoreMap[it]?.rawSharpness ?: 0.0
            }
            val sortedIds = raw.photoIds.sortedWith(scoreComparator)
            val bestId = sortedIds.first()
            val recommendedCount = when (raw.photoIds.size) {
                in 2..3 -> 0
                in 4..6 -> 1
                else    -> 2
            }
            val recommendedIds = sortedIds.drop(1).take(recommendedCount)
            BurstGroup(
                groupId = "burst_$bestId",
                groupIndex = raw.groupIndex,
                photoIds = raw.photoIds,
                bestPhotoId = bestId,
                recommendedPhotoIds = recommendedIds,
                bestScore = scoreMap[bestId]?.totalScore ?: 0.0
            )
        }
    }

    /**
     * 기존 호환용: 1단계 + 2단계를 한번에 수행.
     */
    suspend fun detect(photos: List<Photo>): List<BurstGroup> {
        val rawGroups = detectGroups(photos)
        val scoreMap = photos.associate { it.id to (it.recommendationScore ?: RecommendationScore()) }
        return selectBestPhotos(rawGroups, scoreMap)
    }

    private fun computeDHash(filePath: String): Long? {
        return try {
            val bitmap = BitmapLoader.loadThumbnail(filePath, DHASH_WIDTH, DHASH_HEIGHT)
                ?: return null

            var hash = 0L
            for (y in 0 until DHASH_HEIGHT) {
                for (x in 0 until DHASH_WIDTH - 1) {
                    val leftPixel = bitmap.getPixel(x, y)
                    val rightPixel = bitmap.getPixel(x + 1, y)
                    val leftGray = grayscale(leftPixel)
                    val rightGray = grayscale(rightPixel)
                    if (leftGray > rightGray) {
                        hash = hash or (1L shl (y * (DHASH_WIDTH - 1) + x))
                    }
                }
            }
            bitmap.recycle()
            hash
        } catch (e: Exception) {
            null
        }
    }

    private fun grayscale(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    private fun hammingDistance(a: Long, b: Long): Int {
        return java.lang.Long.bitCount(a xor b)
    }

    companion object {
        const val CHAIN_TIME_MS = 10_000L
        const val FAST_BURST_TIME_MS = 3_000L
        const val DHASH_THRESHOLD = 12
        const val DHASH_THRESHOLD_RELAXED = 18
        const val MIN_GROUP_SIZE = 2
        private const val DHASH_WIDTH = 9
        private const val DHASH_HEIGHT = 8
        private const val TAG = "BurstDetection"
    }
}

/** dHash 기반 Raw 그룹 (점수 없음) */
data class RawBurstGroup(
    val groupIndex: Int,
    val photoIds: List<Long>
)
