package com.cola.pickly.feature.archive.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.components.EmptyStateScreen

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
    onNavigateToOrganize: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateScreen(
        titleRes = R.string.archive_empty_title,
        subtitleRes = R.string.archive_empty_subtitle,
        ctaTextRes = R.string.archive_empty_cta,
        onCtaClick = onNavigateToOrganize,
        modifier = modifier
    )
}

