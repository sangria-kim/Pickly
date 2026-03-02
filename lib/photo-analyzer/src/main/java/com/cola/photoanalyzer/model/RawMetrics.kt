package com.cola.photoanalyzer.model

data class RawMetrics(
    val laplacianVariance: Double = 0.0,
    val faceLaplacianVariance: Double = 0.0,
    val eyeOpenProb: Double = 0.0,
    val leftEyeOpenProb: Double = 0.0,
    val rightEyeOpenProb: Double = 0.0,
    val smileProb: Double = 0.0,
    val allEyesOpenProbs: List<Double> = emptyList(),
    val allSmileProbs: List<Double> = emptyList(),
    val minEyeOpenProb: Double = 0.0,
    val avgEyeOpenProb: Double = 0.0
)
