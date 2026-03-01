package com.cola.pickly.feature.organize.components

import androidx.compose.ui.graphics.Color

/**
 * 디버그 오버레이에서 연사 그룹별 색상을 제공하는 팔레트.
 */
object BurstDebugColors {
    private val palette = listOf(
        Color(0xFFE53935), // Red
        Color(0xFF1E88E5), // Blue
        Color(0xFF43A047), // Green
        Color(0xFFF57C00), // Orange
        Color(0xFF8E24AA), // Purple
        Color(0xFF00ACC1), // Cyan
        Color(0xFFFFB300), // Amber
        Color(0xFF6D4C41), // Brown
    )

    fun forIndex(index: Int): Color = palette[index % palette.size]
}
