package com.cola.pickly.feature.archive.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.theme.TealAccent

/**
 * S-06 아카이브 화면 전용 Top Bar
 * 
 * Wireframe.md S-06 참고:
 * - 좌측: "모아보기" 타이틀
 * - 우측: 필터 아이콘 (V1에서는 "폴더별" 옵션만, 향후 확장 가능)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveTopBar(
    onFilterClick: () -> Unit = {}
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = Modifier.statusBarsPadding(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.outline
        ),
        title = {
            Text(
                text = stringResource(R.string.archive_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        actions = {
            // 필터 버튼
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(
                        Icons.Outlined.ViewAgenda,
                        contentDescription = "Filter"
                    )
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.width(156.dp)) {
                        // V1에서는 "폴더별" 옵션만 제공 (향후 확장 가능)
                        ArchiveFilterItem(
                            label = "폴더별",
                            selected = true,
                            onClick = {
                                onFilterClick()
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }
        }
    )
}

/**
 * 아카이브 필터 아이템 (정리하기 탭의 스타일과 통일)
 */
@Composable
private fun ArchiveFilterItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else Color.Transparent
    val labelColor = if (selected) TealAccent else MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor
        )
    }
}

