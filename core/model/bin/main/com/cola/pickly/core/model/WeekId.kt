package com.cola.pickly.core.model

/**
 * 특정 '1주'를 표현하는 ID.
 * 예: 2025년 49주차 → year = 2025, weekOfYear = 49
 * 
 * Note: Navigation에서 사용하기 위해 Parcelable이 필요하면
 * Feature 모듈에서 확장 함수로 제공하거나, 별도의 Android 모듈로 분리할 수 있습니다.
 */
data class WeekId(
    val year: Int,
    val weekOfYear: Int
) {
    override fun toString(): String = "$year-W$weekOfYear"
}
