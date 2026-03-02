package com.cola.photoanalyzer.model

/** 0-100 범위 품질 점수 */
data class QualityScore(
    val total: Double = 0.0,
    val sharpness: Double = 0.0,
    val expression: Double = 0.0,
    val lighting: Double = 0.0,
    val composition: Double = 0.0
)
