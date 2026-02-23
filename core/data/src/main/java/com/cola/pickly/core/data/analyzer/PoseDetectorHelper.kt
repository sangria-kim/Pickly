package com.cola.pickly.core.data.analyzer

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google ML Kit Pose Detection 기능을 래핑한 헬퍼 클래스입니다.
 * 얼굴이 감지되지 않을 때 인체 포즈를 2차 확인하여 NO_FACE 오탐을 방지합니다.
 */
class PoseDetectorHelper {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
        .build()

    private val detector: PoseDetector = PoseDetection.getClient(options)

    /**
     * Bitmap에서 인체 포즈가 감지되는지 확인합니다.
     *
     * @param bitmap 분석할 이미지
     * @param confidenceThreshold 랜드마크의 최소 inFrameLikelihood (기본 0.5f)
     * @param minLandmarkCount 포즈로 판정하기 위한 최소 유효 랜드마크 수 (기본 5)
     * @return 포즈가 감지되면 true
     */
    suspend fun hasPose(
        bitmap: Bitmap,
        confidenceThreshold: Float = 0.5f,
        minLandmarkCount: Int = 5
    ): Boolean {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val pose = detectPose(image)
            val validLandmarks = pose?.allPoseLandmarks?.count {
                it.inFrameLikelihood >= confidenceThreshold
            } ?: 0
            validLandmarks >= minLandmarkCount
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun detectPose(image: InputImage) = suspendCancellableCoroutine { cont ->
        detector.process(image)
            .addOnSuccessListener { pose ->
                cont.resume(pose)
            }
            .addOnFailureListener {
                cont.resume(null)
            }
    }
}
