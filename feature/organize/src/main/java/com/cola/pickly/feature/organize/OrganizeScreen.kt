package com.cola.pickly.feature.organize

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.state.ToggleableState
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
import com.cola.pickly.feature.organize.components.OrganizeFolderSectionHeader
import com.cola.pickly.feature.organize.components.OrganizeGridScreen
import com.cola.pickly.feature.organize.components.OrganizeTopBar
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.feature.organize.components.BurstDebugInfo
import com.cola.pickly.core.ui.components.BurstRecommendRank
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
    val debugOptions by viewModel.debugOptions.collectAsStateWithLifecycle()
    val folderSelectState by folderSelectViewModel.uiState.collectAsStateWithLifecycle()
    val showSmartOrganizeDialog by viewModel.showSmartOrganizeDialog.collectAsStateWithLifecycle()
    val showInterruptDialog by viewModel.showInterruptDialog.collectAsStateWithLifecycle()
    val showDeleteConfirm by viewModel.showDeleteConfirm.collectAsStateWithLifecycle()
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

    val storageAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onStorageAccessResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(Unit) {
        viewModel.storageAccessRequests.collectLatest { intentSender ->
            storageAccessLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
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
                        hasSelectedFolder = true,
                        isMultiSelectMode = state.isMultiSelectMode,
                        selectedCount = state.selectedCount,
                        photos = state.photos,
                        selectedIds = state.selectedIds,
                        selectionMap = state.selectionMap,
                        isSmartOrganizing = state.isSmartOrganizing,
                        activePhotoFilter = state.activePhotoFilter,
                        sortOrder = sortOrder,
                        onSortToggle = { viewModel.toggleSortOrder() },
                        onSelectAllToggle = { viewModel.toggleSelectAll() },
                        onAcceptedToggle = { viewModel.toggleAcceptedSelection() },
                        onRejectedToggle = { viewModel.toggleRejectedSelection() },
                        onCancelSelection = { viewModel.exitMultiSelectMode() },
                        onSmartOrganizeClick = { viewModel.handleSmartOrganizeIconClick() }
                    )
                }
                is OrganizeUiState.EmptyFolder -> {
                    OrganizeTopBar(
                        hasSelectedFolder = true,
                        isMultiSelectMode = false,
                        selectedCount = 0,
                        photos = emptyList(),
                        selectedIds = emptySet(),
                        selectionMap = emptyMap(),
                        sortOrder = sortOrder,
                        onSortToggle = { viewModel.toggleSortOrder() },
                        onSelectAllToggle = { viewModel.toggleSelectAll() },
                        onAcceptedToggle = { viewModel.toggleAcceptedSelection() },
                        onRejectedToggle = { viewModel.toggleRejectedSelection() },
                        onCancelSelection = { viewModel.exitMultiSelectMode() }
                    )
                }
                else -> {
                    OrganizeTopBar(
                        hasSelectedFolder = false,
                        isMultiSelectMode = false,
                        selectedCount = 0,
                        photos = emptyList(),
                        selectedIds = emptySet(),
                        selectionMap = emptyMap(),
                        sortOrder = sortOrder,
                        onSortToggle = { viewModel.toggleSortOrder() },
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
                    val allPhotoIds = state.photos.map { it.id }.toSet()
                    val selectAllState = when {
                        allPhotoIds.isEmpty() -> ToggleableState.Off
                        state.selectedIds.containsAll(allPhotoIds) -> ToggleableState.On
                        state.selectedIds.isEmpty() -> ToggleableState.Off
                        else -> ToggleableState.Indeterminate
                    }
                    val burstRecommendMap = remember(state.burstGroups) {
                        buildMap {
                            state.burstGroups.forEach { group ->
                                put(group.bestPhotoId, BurstRecommendRank.Best)
                                group.recommendedPhotoIds.forEach { put(it, BurstRecommendRank.Recommended) }
                            }
                        }
                    }
                    val burstDebugMap = remember(state.burstGroupByPhotoId, debugOptions) {
                        if (debugOptions.showDebugOverlay && debugOptions.showBurstGroupOverlay) {
                            state.burstGroupByPhotoId.entries.mapNotNull { (photoId, group) ->
                                val rank = group.rankOf(photoId) ?: return@mapNotNull null
                                photoId to BurstDebugInfo(
                                    groupLabel = "G${group.groupIndex + 1}",
                                    groupIndex = group.groupIndex,
                                    rankInGroup = rank,
                                    isBest = group.bestPhotoId == photoId
                                )
                            }.toMap()
                        } else emptyMap()
                    }
                    OrganizeGridScreen(
                        selectedFolderName = state.folderName,
                        photos = state.displayedPhotos,
                        selectedIds = state.selectedIds,
                        selectionMap = state.selectionMap,
                        isMultiSelectMode = state.isMultiSelectMode,
                        isSmartOrganizing = state.isSmartOrganizing,
                        rejectCandidates = state.rejectCandidates,
                        burstRecommendMap = burstRecommendMap,
                        burstDebugMap = burstDebugMap,
                        gridState = gridState,
                        selectAllState = selectAllState,
                        onFolderSelectClick = {
                            viewModel.requestInterruptConfirmation {
                                showFolderSheet = true
                            }
                        },
                        onPhotoClick = { photo ->
                            viewModel.requestInterruptConfirmation {
                                onNavigateToPhotoDetail(
                                    state.folderId,
                                    photo.id,
                                    state.selectionMap,
                                    false,
                                    state.rejectCandidates,
                                    state.displayedPhotos.map { it.id }
                                )
                            }
                        },
                        onToggleSelection = { photoId ->
                            viewModel.toggleSelection(photoId)
                        },
                        onSelectAllToggle = { viewModel.toggleSelectAll() }
                    )
                }
                is OrganizeUiState.EmptyFolder -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        OrganizeFolderSectionHeader(
                            folderName = state.folderName,
                            onFolderSelectClick = {
                                viewModel.requestInterruptConfirmation {
                                    showFolderSheet = true
                                }
                            },
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                        Text(
                            text = "선택된 폴더에 사진이 없습니다.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
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

        if (showSmartOrganizeDialog) {
            val isSmartOrganizing = (uiState as? OrganizeUiState.GridReady)?.isSmartOrganizing ?: false
            AlertDialog(
                onDismissRequest = {
                    if (!isSmartOrganizing) {
                        viewModel.dismissSmartOrganizeDialog()
                    }
                },
                text = { Text(text = "스마트 정리를 시작할까요?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.dismissSmartOrganizeDialog()
                            viewModel.startSmartOrganize()
                        },
                        enabled = !isSmartOrganizing
                    ) {
                        Text(text = "시작하기")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissSmartOrganizeDialog() },
                        enabled = !isSmartOrganizing
                    ) {
                        Text(text = "취소")
                    }
                }
            )
        }

        if (showInterruptDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissInterruptDialog() },
                text = { Text(text = "스마트 정리를 중단하고 계속할까요?") },
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
                        Text(text = "계속 정리")
                    }
                }
            )
        }

        if (showDeleteConfirm) {
            val deleteSelectedCount = (uiState as? OrganizeUiState.GridReady)?.selectedCount ?: 0
            AlertDialog(
                onDismissRequest = {
                    if (!isActionInProgress) viewModel.dismissDeleteConfirmation()
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteSelectedPhotos() },
                        enabled = !isActionInProgress
                    ) { Text(text = stringResource(R.string.delete_confirm_button)) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissDeleteConfirmation() },
                        enabled = !isActionInProgress
                    ) { Text(text = stringResource(R.string.delete_confirm_cancel)) }
                },
                text = {
                    Text(text = stringResource(R.string.delete_confirm_message, deleteSelectedCount))
                }
            )
        }

    }
}
