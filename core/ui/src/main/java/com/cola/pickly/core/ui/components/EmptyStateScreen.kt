package com.cola.pickly.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.theme.TextSecondary

/**
 * 공통 Empty State 화면 컴포넌트
 *
 * 콘텐츠가 없을 때 표시되는 화면으로, 제목, 부제목, CTA 버튼으로 구성됩니다.
 *
 * @param titleRes 제목 문자열 리소스 ID
 * @param subtitleRes 부제목 문자열 리소스 ID
 * @param ctaTextRes CTA 버튼 텍스트 리소스 ID
 * @param onCtaClick CTA 버튼 클릭 콜백
 * @param modifier Modifier
 *
 * 사용 예시:
 * ```
 * EmptyStateScreen(
 *     titleRes = R.string.organize_empty_title,
 *     subtitleRes = R.string.organize_empty_subtitle,
 *     ctaTextRes = R.string.organize_empty_cta,
 *     onCtaClick = { /* 폴더 선택 */ }
 * )
 * ```
 */
@Composable
fun EmptyStateScreen(
    @StringRes titleRes: Int,
    @StringRes subtitleRes: Int,
    @StringRes ctaTextRes: Int,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Title
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Button
            Button(
                onClick = onCtaClick,
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(ctaTextRes),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
