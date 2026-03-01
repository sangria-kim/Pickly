package com.cola.pickly.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

enum class BurstRecommendRank(val label: String) {
    Best("추천"),
    RunnerUp("후보")
}

@Composable
fun BurstRecommendBadge(
    rank: BurstRecommendRank,
    modifier: Modifier = Modifier
) {
    when (rank) {
        BurstRecommendRank.Best -> PillBadge(
            text = rank.label,
            backgroundColor = Color(0xE6E8F5E9),
            textColor = Color(0xFF2E7D32),
            modifier = modifier
        )
        BurstRecommendRank.RunnerUp -> PillBadge(
            text = rank.label,
            backgroundColor = Color(0x99E8F5E9),
            textColor = Color(0xFF4CAF50),
            modifier = modifier
        )
    }
}
