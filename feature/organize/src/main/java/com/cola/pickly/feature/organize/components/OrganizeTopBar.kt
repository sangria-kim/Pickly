package com.cola.pickly.feature.organize.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.theme.TealAccent
import com.cola.pickly.core.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeTopBar(
    selectedFolderName: String?,
    isMultiSelectMode: Boolean = false,
    selectedCount: Int = 0,
    photos: List<Photo> = emptyList(),
    selectedIds: Set<Long> = emptySet(),
    selectionMap: Map<Long, PhotoSelectionState> = emptyMap(),
    onFolderSelectClick: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onAcceptedToggle: () -> Unit,
    onRejectedToggle: () -> Unit,
    onCancelSelection: () -> Unit = {}
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    // BackHandler: 팝업이 열려있을 때 Back 버튼으로 닫기
    BackHandler(enabled = showFilterMenu) {
        showFilterMenu = false
    }

    // 체크박스 상태 계산
    val selectAllState = remember(selectedIds, photos) {
        val allPhotoIds = photos.map { it.id }.toSet()
        when {
            allPhotoIds.isEmpty() -> ToggleableState.Off
            selectedIds.containsAll(allPhotoIds) -> ToggleableState.On
            selectedIds.isEmpty() -> ToggleableState.Off
            else -> ToggleableState.Indeterminate
        }
    }

    val acceptedPhotoIds = remember(selectionMap) {
        selectionMap.filter { it.value == PhotoSelectionState.Selected }.keys.toSet()
    }

    val rejectedPhotoIds = remember(selectionMap) {
        selectionMap.filter { it.value == PhotoSelectionState.Rejected }.keys.toSet()
    }

    val isAcceptedChecked = remember(selectedIds, acceptedPhotoIds) {
        acceptedPhotoIds.isNotEmpty() && selectedIds.containsAll(acceptedPhotoIds)
    }

    val isRejectedChecked = remember(selectedIds, rejectedPhotoIds) {
        rejectedPhotoIds.isNotEmpty() && selectedIds.containsAll(rejectedPhotoIds)
    }

    val hasAcceptedPhotos = remember(acceptedPhotoIds) {
        acceptedPhotoIds.isNotEmpty()
    }

    val hasRejectedPhotos = remember(rejectedPhotoIds) {
        rejectedPhotoIds.isNotEmpty()
    }

    // Multi Select Mode일 때는 배경색을 앱 대표 색상으로 변경
    val containerColor = if (isMultiSelectMode)
        TealAccent
    else
        MaterialTheme.colorScheme.surface

    val contentColor = if (isMultiSelectMode)
        Color.White
    else
        MaterialTheme.colorScheme.onSurface

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
                    IconButton(onClick = { showFilterMenu = !showFilterMenu }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = contentColor
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        // 전체 선택 (Tri-state)
                        FilterTriStateCheckboxItem(
                            label = stringResource(R.string.filter_select_all),
                            state = selectAllState,
                            onToggle = onSelectAllToggle
                        )

                        // 채택
                        FilterCheckboxItem(
                            label = stringResource(R.string.filter_accepted),
                            checked = isAcceptedChecked,
                            enabled = hasAcceptedPhotos,
                            onToggle = onAcceptedToggle
                        )

                        // 제외
                        FilterCheckboxItem(
                            label = stringResource(R.string.filter_rejected),
                            checked = isRejectedChecked,
                            enabled = hasRejectedPhotos,
                            onToggle = onRejectedToggle
                        )
                    }
                }
            }
        }
    )
}

/**
 * 일반 체크박스 아이템
 */
@Composable
private fun FilterCheckboxItem(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = TealAccent
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

/**
 * Tri-state 체크박스 아이템
 */
@Composable
private fun FilterTriStateCheckboxItem(
    label: String,
    state: ToggleableState,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TriStateCheckbox(
            state = state,
            onClick = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = TealAccent
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

enum class FilterOption {
    ALL, SELECTED, NOT_SELECTED
}
