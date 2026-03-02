package com.cola.photoanalyzer.detection

import android.graphics.Bitmap

interface PoseDetector {
    suspend fun hasPose(
        bitmap: Bitmap,
        confidenceThreshold: Float = 0.5f,
        minLandmarkCount: Int = 5
    ): Boolean
}
