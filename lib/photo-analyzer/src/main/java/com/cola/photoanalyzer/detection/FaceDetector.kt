package com.cola.photoanalyzer.detection

import android.graphics.Bitmap
import com.cola.photoanalyzer.model.BoundingBox

interface FaceDetector {
    suspend fun detect(bitmap: Bitmap): List<DetectedFace>
}

data class DetectedFace(
    val boundingBox: BoundingBox,
    val leftEyeOpenProb: Float?,
    val rightEyeOpenProb: Float?,
    val smileProb: Float?,
    val hasNose: Boolean,
    val hasMouthLeft: Boolean,
    val hasMouthRight: Boolean,
    val hasMouthBottom: Boolean,
    val leftEyePosition: Point?,
    val rightEyePosition: Point?
)

data class Point(val x: Float, val y: Float)
