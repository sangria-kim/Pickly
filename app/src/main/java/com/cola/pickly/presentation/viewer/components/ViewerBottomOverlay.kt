package com.cola.pickly.presentation.viewer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.ui.components.RejectReasonBadge

@Composable
fun ViewerBottomOverlay(
    isSelected: Boolean,
    isRejected: Boolean,
    rejectReason: RejectReason? = null,
    onSelectClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                )
            )
            .navigationBarsPadding()
            .padding(vertical = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 채택 버튼 (check_circle) - Left
            ViewerControlButton(
                icon = Icons.Outlined.Check,
                isActive = isSelected,
                activeColor = Color(0xFF2ED3B7), // Requested: #2ED3B7
                contentDescription = "Select",
                onClick = onSelectClick
            )

            Spacer(modifier = Modifier.width(48.dp))

            // 제외 버튼 + pill 배지 영역
            Box(contentAlignment = Alignment.TopCenter) {
                ViewerControlButton(
                    icon = Icons.Outlined.Close,
                    isActive = isRejected,
                    activeColor = Color(0xFFFF5252), // Requested: #FF5252
                    contentDescription = "Reject",
                    onClick = onRejectClick
                )

                // pill 배지 (조건부 표시)
                rejectReason?.let { reason ->
                    RejectReasonBadge(
                        reason = reason,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-32).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewerControlButton(
    icon: ImageVector,
    isActive: Boolean,
    activeColor: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    // 활성화 시 70% 불투명, 비활성화 시 35% 불투명 (기존 대비 30% 더 투명)
    val backgroundColor = if (isActive) {
        activeColor.copy(alpha = 0.7f)
    } else {
        Color.Gray.copy(alpha = 0.35f)
    }

    val iconTint = Color.White

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, 
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(32.dp)
        )
    }
}
