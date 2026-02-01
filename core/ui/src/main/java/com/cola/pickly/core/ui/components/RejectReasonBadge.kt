package com.cola.pickly.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cola.pickly.core.model.RejectReason

/**
 * 스마트 자동 제외 사유를 표시하는 Pill 배지 컴포넌트
 *
 * PillBadge를 활용하여 스마트 제외 전용 스타일을 적용합니다.
 *
 * @param reason 제외 사유 (RejectReason enum)
 * @param modifier Modifier
 */
@Composable
fun RejectReasonBadge(
    reason: RejectReason,
    modifier: Modifier = Modifier
) {
    PillBadge(
        text = reason.label,
        backgroundColor = Color(0xE6FFE7EA), // 연한 red tint (90% opacity)
        textColor = Color(0xFFE5484D),       // 딥 레드
        modifier = modifier
    )
}
