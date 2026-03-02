package com.cola.photoanalyzer.internal

import android.graphics.Bitmap
import com.cola.photoanalyzer.detection.DetectedFace
import com.cola.photoanalyzer.detection.PoseDetector
import com.cola.photoanalyzer.model.AnalysisConfig
import com.cola.photoanalyzer.model.DefectType
import kotlin.math.abs

/**
 * 우선순위 기반 결함 체크 체인.
 * NO_FACE → TOO_SMALL → CROPPED → EYES_CLOSED → OCCLUDED → BLURRY
 */
internal object DefectChecker {

    /**
     * @param allDetectedFaces ML Kit이 검출한 모든 얼굴
     * @param validFaces minFaceRatio를 통과한 유효 얼굴 (null = allDetectedFaces가 비어있음)
     * @param bestFace 유효 얼굴 중 가장 큰 얼굴 (null = validFaces가 비어있거나 null)
     * @param bitmap 분석 비트맵
     * @param sharpnessRaw 전체 Laplacian variance
     * @param config 분석 설정
     * @param poseDetector 포즈 검출기
     * @return 결함 유형 (null이면 통과)
     */
    suspend fun check(
        allDetectedFaces: List<DetectedFace>,
        validFaces: List<DetectedFace>?,
        bestFace: DetectedFace?,
        bitmap: Bitmap,
        sharpnessRaw: Double,
        config: AnalysisConfig,
        poseDetector: PoseDetector
    ): DefectType? {
        val enabled = config.enabledDefects

        // 1. NO_FACE
        if (allDetectedFaces.isEmpty()) {
            val hasPose = poseDetector.hasPose(bitmap, POSE_CONFIDENCE_THRESHOLD, POSE_MIN_LANDMARK_COUNT)
            if (hasPose) return null // 포즈 감지됨 → 풍경 처리
            return if (DefectType.NO_FACE in enabled) DefectType.NO_FACE else null
        }

        // 2. TOO_SMALL
        if (validFaces.isNullOrEmpty()) {
            return if (DefectType.TOO_SMALL in enabled) DefectType.TOO_SMALL else null
        }

        if (bestFace == null) return null

        // 3. CROPPED
        if (DefectType.CROPPED in enabled && isFaceCropped(bestFace, bitmap.width, bitmap.height)) {
            return DefectType.CROPPED
        }

        // 4. EYES_CLOSED
        if (DefectType.EYES_CLOSED in enabled) {
            val leftEye = bestFace.leftEyeOpenProb ?: 0f
            val rightEye = bestFace.rightEyeOpenProb ?: 0f
            val smile = bestFace.smileProb ?: 0f

            val bothClosed = leftEye < config.eyeOpenThreshold && rightEye < config.eyeOpenThreshold
            val oneVeryClosed = leftEye < SINGLE_EYE_STRICT_THRESHOLD ||
                rightEye < SINGLE_EYE_STRICT_THRESHOLD
            val asymmetry = abs(leftEye - rightEye)
            val asymmetricBlink = oneVeryClosed && asymmetry > EYE_ASYMMETRY_THRESHOLD
            val isEyesClosed = bothClosed || asymmetricBlink
            val isSmiling = smile > config.smileExceptionThreshold

            if (isEyesClosed && !isSmiling) {
                val isSunglasses = bestFace.leftEyePosition != null && bestFace.rightEyePosition != null &&
                    ImageMetrics.isSunglassesLikely(
                        bitmap,
                        bestFace.leftEyePosition.x, bestFace.leftEyePosition.y,
                        bestFace.rightEyePosition.x, bestFace.rightEyePosition.y,
                        bestFace.boundingBox.right - bestFace.boundingBox.left
                    )
                if (!isSunglasses) return DefectType.EYES_CLOSED
            }
        }

        // 5. OCCLUDED
        if (DefectType.OCCLUDED in enabled && isFaceOccluded(bestFace)) {
            return DefectType.OCCLUDED
        }

        // 6. BLURRY
        if (DefectType.BLURRY in enabled && sharpnessRaw < config.blurThreshold.toDouble()) {
            return DefectType.BLURRY
        }

        return null
    }

    private fun isFaceCropped(face: DetectedFace, imgWidth: Int, imgHeight: Int): Boolean {
        val box = face.boundingBox
        return box.left <= 0 || box.top <= 0 ||
            box.right >= imgWidth || box.bottom >= imgHeight
    }

    private fun isFaceOccluded(face: DetectedFace): Boolean {
        return !face.hasNose || !face.hasMouthLeft || !face.hasMouthRight || !face.hasMouthBottom
    }

    private const val POSE_CONFIDENCE_THRESHOLD = 0.5f
    private const val POSE_MIN_LANDMARK_COUNT = 5
    private const val SINGLE_EYE_STRICT_THRESHOLD = 0.10f
    private const val EYE_ASYMMETRY_THRESHOLD = 0.4f
}
