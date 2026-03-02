package com.cola.photoanalyzer.detection.mlkit

import android.graphics.Bitmap
import com.cola.photoanalyzer.detection.PoseDetector
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MlKitPoseDetector : PoseDetector {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)

    override suspend fun hasPose(
        bitmap: Bitmap,
        confidenceThreshold: Float,
        minLandmarkCount: Int
    ): Boolean {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val pose = suspendCancellableCoroutine { cont ->
                detector.process(image)
                    .addOnSuccessListener { pose -> cont.resume(pose) }
                    .addOnFailureListener { cont.resume(null) }
            }
            val validLandmarks = pose?.allPoseLandmarks?.count {
                it.inFrameLikelihood >= confidenceThreshold
            } ?: 0
            validLandmarks >= minLandmarkCount
        } catch (e: Exception) {
            false
        }
    }
}
