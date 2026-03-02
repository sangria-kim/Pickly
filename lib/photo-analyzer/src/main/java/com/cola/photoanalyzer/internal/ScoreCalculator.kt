package com.cola.photoanalyzer.internal

import android.graphics.Bitmap
import com.cola.photoanalyzer.detection.DetectedFace
import com.cola.photoanalyzer.model.QualityScore

internal object ScoreCalculator {

    fun calculateSoloScore(
        bitmap: Bitmap,
        bestFace: DetectedFace,
        faceSharpnessRaw: Double,
        smileProb: Double,
        eyeOpenProb: Double
    ): QualityScore {
        val sharpness = ImageMetrics.normalizeSharpness(faceSharpnessRaw)
        val expression = ((smileProb * 0.6 + eyeOpenProb * 0.4) * 100).coerceIn(0.0, 100.0)
        val lighting = ImageMetrics.calculateLighting(bitmap, bestFace.boundingBox)
        val centerX = (bestFace.boundingBox.left + bestFace.boundingBox.right) / 2.0
        val centerY = (bestFace.boundingBox.top + bestFace.boundingBox.bottom) / 2.0
        val composition = ImageMetrics.calculateComposition(bitmap, centerX, centerY)

        val total = (sharpness * 0.35) +
            (expression * 0.30) +
            (lighting * 0.20) +
            (composition * 0.15)

        return QualityScore(
            total = total.coerceIn(0.0, 100.0),
            sharpness = sharpness,
            expression = expression,
            lighting = lighting,
            composition = composition
        )
    }

    fun calculateGroupScore(
        bitmap: Bitmap,
        bestFace: DetectedFace,
        allValidFaces: List<DetectedFace>,
        rawSharpness: Double,
        minEyeOpen: Double
    ): QualityScore {
        val sharpness = ImageMetrics.normalizeSharpness(rawSharpness)
        val minEyeOpenScore = (minEyeOpen * 100).coerceIn(0.0, 100.0)

        val avgExpressionScores = allValidFaces.map { face ->
            val smile = face.smileProb?.toDouble() ?: 0.0
            val leftEye = face.leftEyeOpenProb?.toDouble() ?: 0.0
            val rightEye = face.rightEyeOpenProb?.toDouble() ?: 0.0
            val eyeOpen = (leftEye + rightEye) / 2.0
            (smile * 0.6 + eyeOpen * 0.4) * 100
        }
        val avgExpression = if (avgExpressionScores.isNotEmpty()) avgExpressionScores.average() else 0.0

        val lighting = ImageMetrics.calculateLighting(bitmap, bestFace.boundingBox)
        val centers = allValidFaces.map { face ->
            Pair(
                (face.boundingBox.left + face.boundingBox.right) / 2.0,
                (face.boundingBox.top + face.boundingBox.bottom) / 2.0
            )
        }
        val composition = ImageMetrics.calculateGroupComposition(bitmap, centers)

        val total = (sharpness * 0.20) +
            (minEyeOpenScore * 0.30) +
            (avgExpression.coerceIn(0.0, 100.0) * 0.25) +
            (lighting * 0.15) +
            (composition * 0.10)

        return QualityScore(
            total = total.coerceIn(0.0, 100.0),
            sharpness = sharpness,
            expression = avgExpression.coerceIn(0.0, 100.0),
            lighting = lighting,
            composition = composition
        )
    }

    fun calculateLandscapeScore(
        bitmap: Bitmap,
        rawSharpness: Double
    ): QualityScore {
        val sharpness = ImageMetrics.normalizeSharpness(rawSharpness)
        val colorContrast = ImageMetrics.calculateColorContrast(bitmap)
        val lightingUniformity = ImageMetrics.calculateLightingUniformity(bitmap)

        val total = (sharpness * 0.50) +
            (colorContrast * 0.30) +
            (lightingUniformity * 0.20)

        return QualityScore(
            total = total.coerceIn(0.0, 100.0),
            sharpness = sharpness,
            expression = 0.0,
            lighting = lightingUniformity,
            composition = colorContrast
        )
    }
}
