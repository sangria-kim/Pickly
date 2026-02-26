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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.components.FilterTriStateCheckboxItem
import com.cola.pickly.core.ui.components.MultiSelectTopBarContent
import com.cola.pickly.core.ui.theme.TealAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveTopBar(
    hasAcceptedPhotos: Boolean = false,
    isMultiSelectMode: Boolean = false,
    selectedCount: Int = 0,
    selectAllState: ToggleableState = ToggleableState.Off,
    onCancelSelection: () -> Unit = {},
    onSelectAllToggle: () -> Unit = {},
    onFilterClick: () -> Unit = {}
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    val containerColor = if (isMultiSelectMode) TealAccent else MaterialTheme.colorScheme.surface
    val contentColor = if (isMultiSelectMode) Color.White else MaterialTheme.colorScheme.onSurface
    val actionIconColor = if (isMultiSelectMode) Color.White else MaterialTheme.colorScheme.outline

    TopAppBar(
        modifier = Modifier.statusBarsPadding(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColor,
            actionIconContentColor = actionIconColor
        ),
        title = {
            if (isMultiSelectMode) {
                MultiSelectTopBarContent(
                    selectedCount = selectedCount,
                    onCancelClick = onCancelSelection,
                    contentColor = contentColor
                )
            } else {
                Text(
                    text = stringResource(R.string.archive_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        actions = {
            if (isMultiSelectMode) {
                // 다중 선택 모드: 전체 선택 체크박스
                Box {
                    IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                        Icon(
                            Icons.Outlined.ViewAgenda,
                            contentDescription = "Select All"
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.width(156.dp)) {
                            FilterTriStateCheckboxItem(
                                label = stringResource(R.string.filter_select_all),
                                state = selectAllState,
                                onToggle = {
                                    onSelectAllToggle()
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            } else if (hasAcceptedPhotos) {
                // Normal Mode: 기존 필터 버튼
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
        }
    )
}

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
