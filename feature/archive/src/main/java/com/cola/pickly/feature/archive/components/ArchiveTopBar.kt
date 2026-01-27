package com.cola.pickly.feature.archive.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.theme.TextPrimary

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
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
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
                        Icons.Default.FilterList,
                        contentDescription = "Filter"
                    )
                }
                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    // V1에서는 "폴더별" 옵션만 제공 (향후 확장 가능)
                    DropdownMenuItem(
                        text = { Text("폴더별", color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            onFilterClick()
                            showFilterMenu = false
                        }
                    )
                }
            }
        }
    )
}

