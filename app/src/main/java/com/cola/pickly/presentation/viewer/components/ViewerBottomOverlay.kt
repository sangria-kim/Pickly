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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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

@Composable
fun ViewerBottomOverlay(
    isSelected: Boolean,
    isRejected: Boolean,
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
            // 채택 버튼 (Check) - Left
            ViewerControlButton(
                icon = Icons.Default.Check,
                isActive = isSelected,
                activeColor = Color(0xFF2ED3B7), // Requested: #2ED3B7
                contentDescription = "Select",
                onClick = onSelectClick
            )

            Spacer(modifier = Modifier.width(48.dp))

            // 제외 버튼 (Trash) - Right
            ViewerControlButton(
                icon = Icons.Default.Delete,
                isActive = isRejected,
                activeColor = Color(0xFFFF5252), // Requested: #FF5252
                contentDescription = "Reject",
                onClick = onRejectClick
            )
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
    // 활성화 시에는 원본 색상(투명도 없음), 비활성화(Gray) 시에는 50% 투명도 적용
    val backgroundColor = if (isActive) {
        activeColor
    } else {
        Color.Gray.copy(alpha = 0.5f)
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
