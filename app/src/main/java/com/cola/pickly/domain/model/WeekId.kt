package com.cola.pickly.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 특정 '1주'를 표현하는 ID.
 * 예: 2025년 49주차 → year = 2025, weekOfYear = 49
 */
@Parcelize
data class WeekId(
    val year: Int,
    val weekOfYear: Int
) : Parcelable {
    override fun toString(): String = "$year-W$weekOfYear"
}