package com.cola.pickly.feature.archive.components

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
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.theme.TextSecondary

/**
 * S-06 아카이브 화면의 Empty State UI
 * 
 * Wireframe.md S-06 참고:
 * - 안내 문구: "아직 모아볼 사진이 없어요"
 * - 부가 문구: "정리하기 탭에서 사진을 골라보세요"
 * - CTA 버튼: "사진 정리하러 가기" (Tab 1로 이동)
 */
@Composable
fun ArchiveEmptyScreen(
    onNavigateToOrganize: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // 제목
            Text(
                text = stringResource(R.string.archive_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 부제목
            Text(
                text = stringResource(R.string.archive_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // CTA 버튼
            Button(
                onClick = onNavigateToOrganize,
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.archive_empty_cta),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

