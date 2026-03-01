package com.cola.pickly.feature.organize.components

/**
 * 디버그 오버레이에서 연사 그룹 정보를 표시하기 위한 DTO.
 *
 * BurstGroup을 직접 전달하지 않고 표시에 필요한 최소 정보만 추출하여
 * recomposition 안정성을 확보합니다.
 */
data class BurstDebugInfo(
    val groupLabel: String,
    val groupIndex: Int,
    val rankInGroup: Int,
    val isBest: Boolean
)
