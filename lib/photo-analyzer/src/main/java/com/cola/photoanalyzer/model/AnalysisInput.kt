package com.cola.photoanalyzer.model

/** 분석할 사진 1장의 입력 정보 */
data class AnalysisInput(
    val id: Long,
    val filePath: String,
    val takenAt: Long = 0L
)
