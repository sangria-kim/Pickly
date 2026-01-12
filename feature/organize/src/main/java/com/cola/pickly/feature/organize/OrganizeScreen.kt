package com.cola.pickly.feature.organize

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import com.cola.pickly.core.model.PhotoFolder
import com.cola.pickly.feature.organize.FolderSelectUiState
import com.cola.pickly.feature.organize.FolderSelectViewModel
import com.cola.pickly.feature.organize.components.FolderSelectScreen
import com.cola.pickly.feature.organize.components.OrganizeEmptyScreen
import com.cola.pickly.feature.organize.components.OrganizeGridScreen
import com.cola.pickly.feature.organize.components.OrganizeTopBar
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.ui.theme.BackgroundWhite
import com.cola.pickly.core.ui.R
import android.content.Intent

@Composable
fun OrganizeScreen(
    viewModel: OrganizeViewModel = hiltViewModel(),
    folderSelectViewModel: FolderSelectViewModel = hiltViewModel(),
    onNavigateToPhotoDetail: (String, Long, Map<Long, PhotoSelectionState>, Boolean) -> Unit,
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
    val showDeleteConfirm by viewModel.showDeleteConfirm.collectAsStateWithLifecycle()
    val isActionInProgress by viewModel.isActionInProgress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedCount = (uiState as? OrganizeUiState.GridReady)?.selectedCount ?: 0
    
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

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.shareEvents.collectLatest { uris ->
            if (uris.isEmpty()) return@collectLatest

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.getString(R.string.bulk_action_share)
                )
            )
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
    
    // Bulk Action 콜백 설정
    LaunchedEffect(Unit) {
        onShareClick?.invoke { if (!isActionInProgress) viewModel.shareSelectedPhotos() }
        onMoveClick?.invoke { if (!isActionInProgress) viewModel.moveSelectedPhotos() }
        onCopyClick?.invoke { if (!isActionInProgress) viewModel.copySelectedPhotos() }
        onDeleteClick?.invoke { if (!isActionInProgress) viewModel.requestDeleteConfirmation() }
    }
    
    BackHandler(enabled = isMultiSelectMode) {
        viewModel.exitMultiSelectMode()
    }

    Scaffold(
        containerColor = BackgroundWhite, // 전체 배경색 흰색 적용
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                        photos = state.photos,
                        selectedIds = state.selectedIds,
                        selectionMap = state.selectionMap,
                        isMultiSelectMode = state.isMultiSelectMode,
                        onPhotoClick = { photo ->
                            onNavigateToPhotoDetail(state.folderId, photo.id, state.selectionMap, false)
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

        if (showDeleteConfirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { 
                    if (!isActionInProgress) {
                        viewModel.dismissDeleteConfirmation()
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.deleteSelectedPhotos() },
                        enabled = !isActionInProgress
                    ) {
                        androidx.compose.material3.Text(
                            text = stringResource(R.string.delete_confirm_button)
                        )
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.dismissDeleteConfirmation() },
                        enabled = !isActionInProgress
                    ) {
                        androidx.compose.material3.Text(
                            text = stringResource(R.string.delete_confirm_cancel)
                        )
                    }
                },
                title = { 
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.delete_confirm_title)
                    )
                },
                text = { 
                    androidx.compose.material3.Text(
                        text = stringResource(
                            R.string.delete_confirm_message,
                            selectedCount
                        )
                    )
                }
            )
        }
    }
}
