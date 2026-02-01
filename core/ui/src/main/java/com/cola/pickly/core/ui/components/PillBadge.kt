package com.cola.pickly.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 범용 Pill 배지 컴포넌트
 *
 * 다양한 pill 배지를 생성할 수 있도록 색상, 텍스트, 크기를 커스터마이징할 수 있습니다.
 *
 * @param text 배지에 표시할 텍스트
 * @param backgroundColor 배지 배경색
 * @param textColor 텍스트 색상
 * @param modifier Modifier
 * @param fontSize 텍스트 크기 (기본: 11sp)
 * @param fontWeight 텍스트 굵기 (기본: SemiBold)
 * @param height 배지 높이 (기본: 24dp)
 */
@Composable
fun PillBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    height: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(percent = 40)
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor,
            maxLines = 1
        )
    }
}
