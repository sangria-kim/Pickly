package com.cola.pickly.feature.organize.domain.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.cola.pickly.core.model.BurstGroup
import com.cola.pickly.core.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 연속 촬영(버스트) 사진 그룹을 감지하는 엔진.
 *
 * 시간 적응형 dHash 체이닝 방식:
 * - ≤ 3초: 완화된 dHash 임계값([DHASH_THRESHOLD_RELAXED])으로 체이닝 (구도 변경 허용)
 * - 3~10초: 기본 dHash 임계값([DHASH_THRESHOLD])으로 체이닝 (유사한 장면만)
 * - > 10초: 체인 끊김 (별개 촬영으로 간주)
 *
 * 그룹 내 베스트 사진은 recommendationScore.totalScore가 가장 높은 사진입니다.
 */
@Singleton
class BurstDetectionEngine @Inject constructor() {

    /**
     * 사진 목록에서 연사 그룹을 감지합니다.
     *
     * @param photos 분석할 사진 목록 (촬영 시각 정렬 권장)
     * @return 감지된 연사 그룹 목록 (2장 이상인 그룹만 반환)
     */
    suspend fun detect(photos: List<Photo>): List<BurstGroup> = withContext(Dispatchers.Default) {
        if (photos.size < 2) return@withContext emptyList()

        val sorted = photos.sortedBy { it.takenAt }

        // 모든 사진의 dHash를 한번에 계산
        val hashes = sorted.map { computeDHash(it.filePath) }

        // 인접 사진 체이닝: 시간 + dHash 동시 조건
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

        // BurstGroup 생성
        groups.mapIndexed { index, groupPhotos ->
            val scoreComparator = compareByDescending<Photo> {
                it.recommendationScore?.totalScore ?: 0.0
            }.thenByDescending {
                it.recommendationScore?.rawSharpness ?: 0.0
            }
            val bestPhoto = groupPhotos.maxWithOrNull(scoreComparator) ?: groupPhotos.first()
            val sortedByScore = groupPhotos.sortedWith(scoreComparator)
            BurstGroup(
                groupId = "burst_${bestPhoto.id}",
                groupIndex = index,
                photoIds = groupPhotos.map { it.id },
                bestPhotoId = bestPhoto.id,
                runnerUpPhotoId = sortedByScore.getOrNull(1)?.id,
                bestScore = bestPhoto.recommendationScore?.totalScore ?: 0.0
            )
        }
    }

    /**
     * 이미지의 dHash(차이 해시)를 계산합니다.
     * 8x9 크기로 축소 후 인접 픽셀 밝기 비교로 64비트 해시 생성.
     */
    private fun computeDHash(filePath: String): Long? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)

            // 작은 이미지로 디코드
            val sampleSize = maxOf(
                options.outWidth / DHASH_DECODE_SIZE,
                options.outHeight / DHASH_DECODE_SIZE,
                1
            )
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null
            val scaled = Bitmap.createScaledBitmap(bitmap, DHASH_WIDTH, DHASH_HEIGHT, true)
            if (scaled != bitmap) bitmap.recycle()

            var hash = 0L
            for (y in 0 until DHASH_HEIGHT) {
                for (x in 0 until DHASH_WIDTH - 1) {
                    val leftPixel = scaled.getPixel(x, y)
                    val rightPixel = scaled.getPixel(x + 1, y)
                    val leftGray = grayscale(leftPixel)
                    val rightGray = grayscale(rightPixel)
                    if (leftGray > rightGray) {
                        hash = hash or (1L shl (y * (DHASH_WIDTH - 1) + x))
                    }
                }
            }
            scaled.recycle()
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
        /** 인접 사진 체이닝 최대 시간 간격 (밀리초) */
        const val CHAIN_TIME_MS = 10_000L

        /** 빠른 연사 시간 간격 (이하이면 dHash 검증 건너뜀) */
        const val FAST_BURST_TIME_MS = 3_000L

        /** dHash 해밍 거리 임계값 (이하이면 유사) */
        const val DHASH_THRESHOLD = 12

        /** 완화된 dHash 해밍 거리 임계값 (3~10초 구간에서 사용) */
        const val DHASH_THRESHOLD_RELAXED = 18

        /** 최소 그룹 크기 */
        const val MIN_GROUP_SIZE = 2

        private const val DHASH_DECODE_SIZE = 64
        private const val DHASH_WIDTH = 9
        private const val DHASH_HEIGHT = 8
        private const val TAG = "BurstDetection"
    }
}
