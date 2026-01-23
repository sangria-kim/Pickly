package com.cola.pickly.feature.organize.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.components.EmptyStateScreen

@Composable
fun OrganizeEmptyScreen(
    onFolderSelectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateScreen(
        titleRes = R.string.organize_empty_title,
        subtitleRes = R.string.organize_empty_subtitle,
        ctaTextRes = R.string.organize_empty_cta,
        onCtaClick = onFolderSelectClick,
        modifier = modifier
    )
}
