package com.cola.photoanalyzer.model

/** 분석 임계값 설정. 모든 필드에 기본값이 있어 AnalysisConfig()만으로 사용 가능 */
data class AnalysisConfig(
    val blurThreshold: Float = 55.0f,
    val minFaceRatio: Float = 0.05f,
    val eyeOpenThreshold: Float = 0.35f,
    val smileExceptionThreshold: Float = 0.60f,
    val enabledDefects: Set<DefectType> = DefectType.entries.toSet(),
    val maxBitmapSize: Int = 1024,
    val algorithmVersion: Int = CURRENT_ALGORITHM_VERSION
) {
    companion object {
        /**
         * 분석 알고리즘 버전.
         * 점수 계산 로직, Laplacian 계산 방식, 가중치 등이 변경될 때 증가시킵니다.
         */
        const val CURRENT_ALGORITHM_VERSION = 1
    }
}
