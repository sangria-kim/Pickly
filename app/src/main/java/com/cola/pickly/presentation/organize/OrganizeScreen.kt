package com.cola.pickly.presentation.organize

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cola.pickly.domain.model.PhotoFolder
import com.cola.pickly.presentation.folderselect.FolderSelectUiState
import com.cola.pickly.presentation.folderselect.FolderSelectViewModel
import com.cola.pickly.presentation.folderselect.components.FolderSelectScreen
import com.cola.pickly.presentation.organize.components.OrganizeEmptyScreen
import com.cola.pickly.presentation.organize.components.OrganizeGridScreen
import com.cola.pickly.presentation.organize.components.OrganizeTopBar
import com.cola.pickly.presentation.viewer.PhotoSelectionState
import com.cola.pickly.ui.theme.BackgroundWhite

@Composable
fun OrganizeScreen(
    viewModel: OrganizeViewModel = hiltViewModel(),
    folderSelectViewModel: FolderSelectViewModel = hiltViewModel(),
    onNavigateToPhotoDetail: (String, Long, Map<Long, PhotoSelectionState>) -> Unit,
    selectedFolder: Pair<String, String>? = null,
    selectionUpdates: Map<Long, PhotoSelectionState>? = null,
    onMultiSelectModeChanged: ((Boolean) -> Unit)? = null,
    onShareClick: (((() -> Unit)) -> Unit)? = null,
    onMoveClick: (((() -> Unit)) -> Unit)? = null,
    onCopyClick: (((() -> Unit)) -> Unit)? = null,
    onDeleteClick: (((() -> Unit)) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderSelectState by folderSelectViewModel.uiState.collectAsStateWithLifecycle()
    
    var showFolderSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedFolder) {
        selectedFolder?.let { (id, name) ->
            viewModel.updateSelectedFolder(folderId = id, folderName = name)
        }
    }

    LaunchedEffect(selectionUpdates) {
        selectionUpdates?.let { updates ->
            viewModel.applySelectionUpdates(updates)
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
    
    // Bulk Action 콜백 설정
    LaunchedEffect(Unit) {
        onShareClick?.invoke { viewModel.shareSelectedPhotos() }
        onMoveClick?.invoke { viewModel.moveSelectedPhotos() }
        onCopyClick?.invoke { viewModel.copySelectedPhotos() }
        onDeleteClick?.invoke { viewModel.deleteSelectedPhotos() }
    }
    
    BackHandler(enabled = isMultiSelectMode) {
        viewModel.exitMultiSelectMode()
    }

    Scaffold(
        containerColor = BackgroundWhite, // 전체 배경색 흰색 적용
        // Bottom Area는 MainScreen에서 관리됨
        // Normal Mode: Bottom Navigation Bar, Multi Select Mode: Bulk Action Bar
        topBar = {
            when (val state = uiState) {
                is OrganizeUiState.GridReady -> {
                    OrganizeTopBar(
                        selectedFolderName = state.folderName,
                        isMultiSelectMode = state.isMultiSelectMode,
                        selectedCount = state.selectedCount,
                        onFolderSelectClick = { showFolderSheet = true },
                        onFilterClick = { viewModel.onFilterChanged(it) },
                        onCancelSelection = { viewModel.exitMultiSelectMode() }
                    )
                }
                is OrganizeUiState.EmptyFolder -> {
                    OrganizeTopBar(
                        selectedFolderName = state.folderName,
                        isMultiSelectMode = false,
                        selectedCount = 0,
                        onFolderSelectClick = { showFolderSheet = true },
                        onFilterClick = { viewModel.onFilterChanged(it) },
                        onCancelSelection = { viewModel.exitMultiSelectMode() }
                    )
                }
                else -> {
                    OrganizeTopBar(
                        selectedFolderName = null,
                        isMultiSelectMode = false,
                        selectedCount = 0,
                        onFolderSelectClick = { showFolderSheet = true },
                        onFilterClick = { viewModel.onFilterChanged(it) },
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
                        photos = state.photos,
                        selectedIds = state.selectedIds,
                        selectionMap = state.selectionMap,
                        isMultiSelectMode = state.isMultiSelectMode,
                        onPhotoClick = { photo ->
                            onNavigateToPhotoDetail(state.folderId, photo.id, state.selectionMap)
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
                    viewModel.updateSelectedFolder(folderId = folder.id, folderName = folder.name)
                    showFolderSheet = false
                }
            )
        }
    }
}
