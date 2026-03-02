package com.cola.photoanalyzer.model

/** 단일 사진 분석 결과 */
data class AnalysisOutput(
    val inputId: Long,
    val hasDefect: Boolean,
    val defectType: DefectType? = null,
    val category: PhotoCategory,
    val score: QualityScore,
    val faces: FaceInfo,
    val metrics: RawMetrics
)
