package com.cola.pickly.feature.organize

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import com.cola.pickly.feature.organize.FolderSelectUiState
import com.cola.pickly.feature.organize.FolderSelectViewModel
import com.cola.pickly.core.ui.components.FolderSelectScreen
import com.cola.pickly.feature.organize.components.OrganizeEmptyScreen
import com.cola.pickly.feature.organize.components.OrganizeGridScreen
import com.cola.pickly.feature.organize.components.OrganizeTopBar
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.ui.R
import com.cola.pickly.core.ui.components.PicklySnackbarHost

@Composable
fun OrganizeScreen(
    viewModel: OrganizeViewModel = hiltViewModel(),
    folderSelectViewModel: FolderSelectViewModel = hiltViewModel(),
    onNavigateToPhotoDetail: (String, Long, Map<Long, PhotoSelectionState>, Boolean, Map<Long, RejectReason>, List<Long>?) -> Unit,
    selectedFolder: Pair<String, String>? = null,
    selectionUpdates: Map<Long, PhotoSelectionState>? = null,
    onMultiSelectModeChanged: ((Boolean) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val folderSelectState by folderSelectViewModel.uiState.collectAsStateWithLifecycle()
    val showAutoRejectDialog by viewModel.showAutoRejectDialog.collectAsStateWithLifecycle()
    val showInterruptDialog by viewModel.showInterruptDialog.collectAsStateWithLifecycle()
    val isActionInProgress by viewModel.isActionInProgress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // sortOrder가 변경되면 gridState를 재생성하여 스크롤 위치를 초기화
    val gridState = key(sortOrder) { rememberLazyGridState() }

    var showFolderSheet by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        folderSelectViewModel.refreshFolders(silent = true)
    }

    LaunchedEffect(selectedFolder) {
        selectedFolder?.let { (id, name) ->
            viewModel.selectFolder(folderId = id, folderName = name)
        }
    }

    LaunchedEffect(selectionUpdates) {
        selectionUpdates?.let { updates ->
            viewModel.applySelectionUpdates(updates)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Multi Select Mode에서 Back 버튼 처리
    // Wireframe.md S-02: 시스템 Back 버튼은 취소 버튼(X)과 동일한 동작
    val isMultiSelectMode = when (val state = uiState) {
        is OrganizeUiState.GridReady -> state.isMultiSelectMode
        else -> false
    }
    
    // isMultiSelectMode 변경 시 MainScreen에 알림
    LaunchedEffect(isMultiSelectMode) {
        onMultiSelectModeChanged?.invoke(isMultiSelectMode)
    }
    
    BackHandler(enabled = isMultiSelectMode) {
        viewModel.exitMultiSelectMode()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { PicklySnackbarHost(hostState = snackbarHostState) },
        // Bottom Area는 MainScreen에서 관리됨
        // Normal Mode: Bottom Navigation Bar, Multi Select Mode: Bulk Action Bar
        topBar = {
            when (val state = uiState) {
                is OrganizeUiState.GridReady -> {
                    OrganizeTopBar(
                        selectedFolderName = state.folderName,
                        isMultiSelectMode = state.isMultiSelectMode,
                        selectedCount = state.selectedCount,
                        photos = state.photos,
                        selectedIds = state.selectedIds,
                        selectionMap = state.selectionMap,
                        isAnalyzing = state.isAnalyzing,
                        activePhotoFilter = state.activePhotoFilter,
                        sortOrder = sortOrder,
                        onSortToggle = { viewModel.toggleSortOrder() },
                        onFolderSelectClick = {
                            viewModel.requestInterruptConfirmation {
                                showFolderSheet = true
                            }
                        },
                        onSelectAllToggle = { viewModel.toggleSelectAll() },
                        onAcceptedToggle = { viewModel.toggleAcceptedSelection() },
                        onRejectedToggle = { viewModel.toggleRejectedSelection() },
                        onCancelSelection = { viewModel.exitMultiSelectMode() },
                        onAutoRejectClick = { viewModel.handleAutoRejectIconClick() }
                    )
                }
                is OrganizeUiState.EmptyFolder -> {
                    OrganizeTopBar(
                        selectedFolderName = state.folderName,
                        isMultiSelectMode = false,
                        selectedCount = 0,
                        photos = emptyList(),
                        selectedIds = emptySet(),
                        selectionMap = emptyMap(),
                        sortOrder = sortOrder,
                        onSortToggle = { viewModel.toggleSortOrder() },
                        onFolderSelectClick = {
                            viewModel.requestInterruptConfirmation {
                                showFolderSheet = true
                            }
                        },
                        onSelectAllToggle = { viewModel.toggleSelectAll() },
                        onAcceptedToggle = { viewModel.toggleAcceptedSelection() },
                        onRejectedToggle = { viewModel.toggleRejectedSelection() },
                        onCancelSelection = { viewModel.exitMultiSelectMode() }
                    )
                }
                else -> {
                    OrganizeTopBar(
                        selectedFolderName = null,
                        isMultiSelectMode = false,
                        selectedCount = 0,
                        photos = emptyList(),
                        selectedIds = emptySet(),
                        selectionMap = emptyMap(),
                        sortOrder = sortOrder,
                        onSortToggle = { viewModel.toggleSortOrder() },
                        onFolderSelectClick = {
                            viewModel.requestInterruptConfirmation {
                                showFolderSheet = true
                            }
                        },
                        onSelectAllToggle = { viewModel.toggleSelectAll() },
                        onAcceptedToggle = { viewModel.toggleAcceptedSelection() },
                        onRejectedToggle = { viewModel.toggleRejectedSelection() },
                        onCancelSelection = { viewModel.exitMultiSelectMode() }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isActionInProgress) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }
            when (val state = uiState) {
                is OrganizeUiState.NoFolderSelected -> {
                    OrganizeEmptyScreen(
                        onFolderSelectClick = { showFolderSheet = true }
                    )
                }
                is OrganizeUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is OrganizeUiState.GridReady -> {
                    OrganizeGridScreen(
                        photos = state.displayedPhotos,
                        selectedIds = state.selectedIds,
                        selectionMap = state.selectionMap,
                        isMultiSelectMode = state.isMultiSelectMode,
                        isAnalyzing = state.isAnalyzing,
                        autoRejectCandidates = state.autoRejectCandidates,
                        gridState = gridState,
                        onPhotoClick = { photo ->
                            viewModel.requestInterruptConfirmation {
                                onNavigateToPhotoDetail(
                                    state.folderId,
                                    photo.id,
                                    state.selectionMap,
                                    false,
                                    state.autoRejectCandidates,
                                    state.displayedPhotos.map { it.id }
                                )
                            }
                        },
                        onToggleSelection = { photoId ->
                            viewModel.toggleSelection(photoId)
                        }
                    )
                }
                is OrganizeUiState.EmptyFolder -> {
                    Text(text = "선택된 폴더에 사진이 없습니다.")
                }
            }
        }

        if (showFolderSheet) {
            val folders = if (folderSelectState is FolderSelectUiState.Success) {
                (folderSelectState as FolderSelectUiState.Success).folders
            } else {
                emptyList()
            }
            
            val isLoading = folderSelectState is FolderSelectUiState.Loading

            FolderSelectScreen(
                folders = folders,
                isLoading = isLoading,
                onClose = { showFolderSheet = false },
                onFolderClick = { folder ->
                    viewModel.selectFolder(folderId = folder.id, folderName = folder.name)
                    showFolderSheet = false
                }
            )
        }

        if (showAutoRejectDialog) {
            val isAnalyzing = (uiState as? OrganizeUiState.GridReady)?.isAnalyzing ?: false
            AlertDialog(
                onDismissRequest = {
                    if (!isAnalyzing) {
                        viewModel.dismissAutoRejectDialog()
                    }
                },
                text = { Text(text = "아쉬운 사진을 자동으로 제외할까요?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.dismissAutoRejectDialog()
                            viewModel.startAutoRejectAnalysis()
                        },
                        enabled = !isAnalyzing
                    ) {
                        Text(text = "제외하기")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissAutoRejectDialog() },
                        enabled = !isAnalyzing
                    ) {
                        Text(text = "취소")
                    }
                }
            )
        }

        if (showInterruptDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissInterruptDialog() },
                text = { Text(text = "분석을 중단하고 계속할까요?") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmInterrupt() }
                    ) {
                        Text(text = "중단하고 계속")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissInterruptDialog() }
                    ) {
                        Text(text = "계속 분석")
                    }
                }
            )
        }

    }
}
