package com.cola.pickly.feature.organize.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.theme.BackgroundWhite
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeTopBar(
    selectedFolderName: String?,
    isMultiSelectMode: Boolean = false,
    selectedCount: Int = 0,
    onFolderSelectClick: () -> Unit,
    onFilterClick: (FilterOption) -> Unit,
    onCancelSelection: () -> Unit = {}
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    // Multi Select Mode일 때는 배경색을 앱 대표 색상으로 변경
    val containerColor = if (isMultiSelectMode) TealAccent else BackgroundWhite
    val contentColor = if (isMultiSelectMode) Color.White else TextPrimary

    TopAppBar(
        modifier = Modifier.statusBarsPadding(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColor,
            actionIconContentColor = contentColor
        ),
        title = {
            if (isMultiSelectMode) {
                // Multi Select Mode: 취소 버튼 + 선택 개수
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCancelSelection,
                        modifier = Modifier.padding(start = 0.dp, end = 0.dp) // 기본 패딩 제거
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "취소",
                            tint = contentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.selected_count_format, selectedCount),
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor
                    )
                }
            } else {
                // Normal Mode: 폴더 선택
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // Ripple 제거
                            onClick = onFolderSelectClick
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedFolderName ?: stringResource(R.string.folder_selection),
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
            }
        },
        actions = {
            // 필터 버튼은 Normal Mode와 Multi Select Mode 모두에서 표시
            if (selectedFolderName != null || isMultiSelectMode) {
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = contentColor
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        containerColor = BackgroundWhite
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.filter_all), color = TextPrimary) },
                            onClick = {
                                onFilterClick(FilterOption.ALL)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.filter_selected), color = TextPrimary) },
                            onClick = {
                                onFilterClick(FilterOption.SELECTED)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.filter_not_selected), color = TextPrimary) },
                            onClick = {
                                onFilterClick(FilterOption.NOT_SELECTED)
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }
        }
    )
}

enum class FilterOption {
    ALL, SELECTED, NOT_SELECTED
}
