package com.cola.pickly.feature.organize.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.theme.BackgroundWhite
import com.cola.pickly.core.ui.theme.Gray100
import com.cola.pickly.core.ui.theme.Gray300
import com.cola.pickly.core.ui.theme.Gray50
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.theme.TextSecondary

@Composable
fun OrganizeEmptyScreen(
    onFolderSelectClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite) // 배경색 흰색 적용
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Ripple 제거
                onClick = onFolderSelectClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Stack
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Background Icon Box
                Box(
                    modifier = Modifier
                        .size(128.dp) // w-32 h-32 -> 128.dp
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Gray100, Gray50)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp), // w-16 h-16 -> 64.dp
                        tint = Gray300
                    )
                }

                // Foreground Accent Icon Box
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 8.dp) // -bottom-2 -right-2 -> offset
                        .size(48.dp) // w-12 h-12 -> 48.dp
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(TealAccent.copy(alpha = 0.2f), TealAccent.copy(alpha = 0.1f))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp), // w-6 h-6 -> 24.dp
                        tint = TealAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) // gap-6 -> 24.dp

            Text(
                text = stringResource(R.string.empty_folder_guide),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
