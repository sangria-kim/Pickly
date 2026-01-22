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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import com.cola.pickly.feature.organize.FolderSelectUiState
import com.cola.pickly.feature.organize.FolderSelectViewModel
import com.cola.pickly.feature.organize.components.CreateFolderDialog
import com.cola.pickly.feature.organize.components.FolderSelectScreen
import com.cola.pickly.feature.organize.components.FolderSelectMode
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
    val destinationMode by viewModel.destinationSelectionMode.collectAsStateWithLifecycle()
    val isActionInProgress by viewModel.isActionInProgress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedCount = (uiState as? OrganizeUiState.GridReady)?.selectedCount ?: 0

    var showFolderSheet by rememberSaveable { mutableStateOf(false) }
    var showDestinationSelectSheet by rememberSaveable { mutableStateOf(false) }
    var showCreateFolderDialog by rememberSaveable { mutableStateOf(false) }

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

    val context = LocalContext.current

    val storageAccessLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onStorageAccessResult(result.resultCode == Activity.RESULT_OK)
        }

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
        viewModel.storageAccessRequests.collectLatest { intentSender ->
            storageAccessLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.moveStorageAccessRequests.collectLatest { intentSender ->
            storageAccessLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
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
        onMoveClick?.invoke {
            if (!isActionInProgress) {
                viewModel.requestDestinationForMove()
                showDestinationSelectSheet = true
            }
        }
        onCopyClick?.invoke {
            if (!isActionInProgress) {
                viewModel.requestDestinationForCopy()
                showDestinationSelectSheet = true
            }
        }
        onDeleteClick?.invoke { if (!isActionInProgress) viewModel.requestDeleteConfirmation() }
    }
    
    BackHandler(enabled = isMultiSelectMode) {
        viewModel.exitMultiSelectMode()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        photos = state.photos,
                        selectedIds = state.selectedIds,
                        selectionMap = state.selectionMap,
                        onFolderSelectClick = { showFolderSheet = true },
                        onSelectAllToggle = { viewModel.toggleSelectAll() },
                        onAcceptedToggle = { viewModel.toggleAcceptedSelection() },
                        onRejectedToggle = { viewModel.toggleRejectedSelection() },
                        onCancelSelection = { viewModel.exitMultiSelectMode() }
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
                        onFolderSelectClick = { showFolderSheet = true },
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
                        onFolderSelectClick = { showFolderSheet = true },
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
                    viewModel.selectFolder(folderId = folder.id, folderName = folder.name)
                    showFolderSheet = false
                }
            )
        }

        if (showDestinationSelectSheet) {
            val folders = if (folderSelectState is FolderSelectUiState.Success) {
                (folderSelectState as FolderSelectUiState.Success).folders
            } else {
                emptyList()
            }

            val isLoading = folderSelectState is FolderSelectUiState.Loading

            FolderSelectScreen(
                folders = folders,
                isLoading = isLoading,
                mode = FolderSelectMode.DestinationSelection,
                onClose = {
                    showDestinationSelectSheet = false
                    viewModel.cancelDestinationSelection()
                },
                onFolderClick = { folder ->
                    when (destinationMode) {
                        is OrganizeViewModel.DestinationSelectionMode.Move -> viewModel.moveToDestination(folder)
                        is OrganizeViewModel.DestinationSelectionMode.Copy -> viewModel.copyToDestination(folder)
                        null -> {}
                    }
                    showDestinationSelectSheet = false
                },
                onCreateFolderClick = { showCreateFolderDialog = true }
            )
        }

        if (showCreateFolderDialog) {
            CreateFolderDialog(
                onDismiss = { showCreateFolderDialog = false },
                onConfirm = { folderName ->
                    showCreateFolderDialog = false
                    showDestinationSelectSheet = false
                    viewModel.createFolderAndExecute(folderName)
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = {
                    if (!isActionInProgress) {
                        viewModel.dismissDeleteConfirmation()
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteSelectedPhotos() },
                        enabled = !isActionInProgress
                    ) {
                        Text(
                            text = stringResource(R.string.delete_confirm_button)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissDeleteConfirmation() },
                        enabled = !isActionInProgress
                    ) {
                        Text(
                            text = stringResource(R.string.delete_confirm_cancel)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.delete_confirm_title)
                    )
                },
                text = {
                    Text(
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
