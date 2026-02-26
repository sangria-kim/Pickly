package com.cola.pickly.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.ui.theme.RejectRed
import com.cola.pickly.core.ui.theme.TealAccent

/**
 * 사진의 채택/제외 상태를 나타내는 상태 배지 컴포넌트.
 *
 * - 아이콘은 항상 원형 배경과 함께 표시됩니다(배경 없는 단독 표기 금지).
 * - Outlined 아이콘을 사용하여 얇은(weight≈200에 준하는) 인상을 유지합니다.
 * - 상태 정보가 없거나 확정되지 않은 경우(None)는 표시하지 않습니다.
 */
@Composable
fun PhotoSelectionBadge(
    selectionState: PhotoSelectionState,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float,
    containerSize: Dp = 24.dp,
    iconSize: Dp = 16.dp
) {
    if (selectionState == PhotoSelectionState.None) return

    val safeAlpha = backgroundAlpha.coerceIn(0f, 1f)

    val (backgroundColor, icon) =
        when (selectionState) {
            PhotoSelectionState.Selected -> TealAccent to Icons.Outlined.Check
            PhotoSelectionState.Rejected -> RejectRed to Icons.Outlined.Close
            PhotoSelectionState.None -> return
        }

    Box(
        modifier = modifier
            .size(containerSize)
            .background(
                color = backgroundColor.copy(alpha = safeAlpha),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

