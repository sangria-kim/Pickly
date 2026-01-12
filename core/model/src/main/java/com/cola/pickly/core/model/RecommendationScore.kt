package com.cola.pickly.core.model

/**
 * 사진 품질 평가 점수를 나타내는 데이터 클래스입니다.
 * 0.0 ~ 100.0 범위의 점수를 가지며, 컷오프(부적합) 여부도 포함합니다.
 * 
 * Note: Android Rect 의존성을 제거하고 순수 데이터로만 표현합니다.
 * 필요시 Feature 모듈에서 Android Rect로 변환할 수 있습니다.
 */
data class RecommendationScore(
    val totalScore: Double = 0.0,
    
    // 세부 점수 항목
    val sharpnessScore: Double = 0.0,   // 선명도
    val expressionScore: Double = 0.0,  // 표정/눈
    val lightingScore: Double = 0.0,    // 조명
    val compositionScore: Double = 0.0, // 구도
    val backgroundScore: Double = 0.0,  // 배경
    
    // 메타 정보
    val faceCount: Int = 0,             // 검출된 얼굴 수
    val isCutoff: Boolean = false,      // 컷오프(추천 제외) 여부
    val cutoffReason: String? = null,   // 컷오프 사유
    
    // 디버깅용 Raw Data (Cutoff 판단 근거)
    val rawSharpness: Double = 0.0,     // Laplacian Variance
    val eyeOpenProb: Double = 0.0,      // 눈 뜸 확률 (평균)
    val leftEyeOpenProb: Double = 0.0,  // 왼쪽 눈 뜸 확률
    val rightEyeOpenProb: Double = 0.0, // 오른쪽 눈 뜸 확률
    val smileProb: Double = 0.0,        // 웃음 확률
    val headEulerAngleX: Float = 0f,    // 고개 숙임/젖힘 (Pitch)
    val headEulerAngleY: Float = 0f,    // 고개 돌림 (Yaw)
    
    // 디버깅용 얼굴 위치 정보 (원본 이미지 기준 좌표) - Rect 대신 순수 데이터로 표현
    val faceBoundingBox: FaceBoundingBox? = null,                  // 대표 얼굴 (점수 계산 대상)
    val allFaceBoundingBoxes: List<FaceBoundingBox> = emptyList(), // 모든 얼굴
    
    // 분석 기준 이미지 크기 (EXIF 회전이 반영된 원본 크기) - 디버깅 오버레이 좌표 보정용
    val analyzedWidth: Int = 0,
    val analyzedHeight: Int = 0
) {
    companion object {
        // 기본값 (평가 전)
        val DEFAULT = RecommendationScore()
    }
}

/**
 * 얼굴 경계 상자 정보를 순수 데이터로 표현합니다.
 * Android Rect 대신 사용합니다.
 */
data class FaceBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
