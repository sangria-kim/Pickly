package com.cola.pickly.core.model

/**
 * 앱 전체에서 사용하는 '한 장의 사진' 도메인 모델.
 * Android Uri, Cursor 등은 절대 넣지 말고 순수 값 타입만 사용.
 */
data class WeeklyPhoto(
    val id: Long,            // MediaStore ID
    val filePath: String,    // 사진 파일 경로
    val takenAt: Long,       // 촬영 시각(epoch millis)
    val width: Int?,         // 해상도 정보 (optional)
    val height: Int?,
    val recommendationScore: RecommendationScore? = null // 품질 점수 객체 (null이면 평가 전)
)
