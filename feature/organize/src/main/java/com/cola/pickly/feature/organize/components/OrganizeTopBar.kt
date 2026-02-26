package com.cola.pickly.feature.organize.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.SortOrder
import com.cola.pickly.feature.organize.PhotoFilter
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.components.FilterCheckboxItem
import com.cola.pickly.core.ui.components.FilterTriStateCheckboxItem
import com.cola.pickly.core.ui.components.MultiSelectTopBarContent
import com.cola.pickly.core.ui.theme.TealAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeTopBar(
    selectedFolderName: String?,
    isMultiSelectMode: Boolean = false,
    selectedCount: Int = 0,
    photos: List<Photo> = emptyList(),
    selectedIds: Set<Long> = emptySet(),
    selectionMap: Map<Long, PhotoSelectionState> = emptyMap(),
    isAnalyzing: Boolean = false,
    activePhotoFilter: PhotoFilter? = null,
    sortOrder: SortOrder = SortOrder.DESCENDING,
    onSortToggle: () -> Unit = {},
    onFolderSelectClick: () -> Unit,
    onSelectAllToggle: () -> Unit,
    onAcceptedToggle: () -> Unit,
    onRejectedToggle: () -> Unit,
    onCancelSelection: () -> Unit = {},
    onAutoRejectClick: () -> Unit = {}
) {
    var showFilterMenu by remember { mutableStateOf(false) }

    // BackHandler: 팝업이 열려있을 때 Back 버튼으로 닫기
    BackHandler(enabled = showFilterMenu) {
        showFilterMenu = false
    }

    // 체크박스 상태 계산
    val acceptedPhotoIds = remember(selectionMap) {
        selectionMap.filter { it.value == PhotoSelectionState.Selected }.keys.toSet()
    }

    val rejectedPhotoIds = remember(selectionMap) {
        selectionMap.filter { it.value == PhotoSelectionState.Rejected }.keys.toSet()
    }

    val selectAllState = remember(isMultiSelectMode, selectedIds, photos, activePhotoFilter) {
        if (!isMultiSelectMode) {
            // 일반 모드: 필터 활성화 여부 표시
            if (activePhotoFilter != null) ToggleableState.Indeterminate else ToggleableState.Off
        } else {
            // 멀티셀렉트 모드: 기존 로직
            val allPhotoIds = photos.map { it.id }.toSet()
            when {
                allPhotoIds.isEmpty() -> ToggleableState.Off
                selectedIds.containsAll(allPhotoIds) -> ToggleableState.On
                selectedIds.isEmpty() -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
        }
    }

    val isAcceptedChecked = remember(isMultiSelectMode, selectedIds, acceptedPhotoIds, activePhotoFilter) {
        if (isMultiSelectMode) {
            acceptedPhotoIds.isNotEmpty() && selectedIds.containsAll(acceptedPhotoIds)
        } else {
            activePhotoFilter is PhotoFilter.Accepted
        }
    }

    val isRejectedChecked = remember(isMultiSelectMode, selectedIds, rejectedPhotoIds, activePhotoFilter) {
        if (isMultiSelectMode) {
            rejectedPhotoIds.isNotEmpty() && selectedIds.containsAll(rejectedPhotoIds)
        } else {
            activePhotoFilter is PhotoFilter.Rejected
        }
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

    val actionIconColor = if (isMultiSelectMode)
        Color.White
    else
        MaterialTheme.colorScheme.outline

    Box {
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                titleContentColor = contentColor,
                actionIconContentColor = actionIconColor
            ),
        title = {
            if (isMultiSelectMode) {
                // Multi Select Mode: 취소 버튼 + 선택 개수
                MultiSelectTopBarContent(
                    selectedCount = selectedCount,
                    onCancelClick = onCancelSelection,
                    contentColor = contentColor
                )
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
            // 스마트 제외 버튼 (폴더 선택됐을 때만, Multi Select Mode 아닐 때만)
            if (selectedFolderName != null && !isMultiSelectMode) {
                IconButton(
                    onClick = onAutoRejectClick,
                    enabled = true  // 분석 중에도 클릭 가능 (취소 용도)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // 항상 AutoAwesome 아이콘 표시
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = if (isAnalyzing) "분석 취소" else "스마트 제외",
                        )
                    }
                }
            }

            // 정렬 토글 버튼 (Normal Mode, Multi Select Mode 모두 표시)
            if (selectedFolderName != null || isMultiSelectMode) {
                IconButton(onClick = onSortToggle, enabled = !isAnalyzing) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = if (sortOrder == SortOrder.DESCENDING) "최신순" else "오래된순",
                        modifier = if (sortOrder == SortOrder.ASCENDING)
                            Modifier.graphicsLayer { scaleY = -1f } else Modifier
                    )
                }
            }

            // 필터 버튼은 Normal Mode와 Multi Select Mode 모두에서 표시
            if (selectedFolderName != null || isMultiSelectMode) {
                Box {
                    IconButton(
                        onClick = { showFilterMenu = !showFilterMenu },
                        enabled = !isAnalyzing  // 분석 중일 때 비활성화
                    ) {
                        Icon(
                            Icons.Default.FilterAlt,
                            contentDescription = "Filter",
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.width(156.dp)) {
                            // 전체 선택 (Tri-state)
                            FilterTriStateCheckboxItem(
                                label = stringResource(R.string.filter_select_all),
                                state = selectAllState,
                                onToggle = onSelectAllToggle
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 2.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
        }
        )

        // Top Bar 하단에 overlay로 프로그레스 바 배치 (높이 변화 없음)
        if (isAnalyzing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp),
                color = TealAccent
            )
        }
    }
}
