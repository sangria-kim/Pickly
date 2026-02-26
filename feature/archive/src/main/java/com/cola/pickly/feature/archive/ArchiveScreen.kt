package com.cola.pickly.feature.archive

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.cola.pickly.feature.archive.components.ArchiveEmptyScreen
import com.cola.pickly.feature.archive.components.ArchiveSection
import com.cola.pickly.feature.archive.components.ArchiveTopBar
import com.cola.pickly.core.model.PhotoFolder
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.ui.R

@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel = hiltViewModel(),
    globalSelectionMap: Map<Long, PhotoSelectionState> = emptyMap(),
    onNavigateToPhotoDetail: (String, Long, Map<Long, PhotoSelectionState>, Boolean, Map<Long, RejectReason>, List<Long>?) -> Unit,
    onNavigateToOrganize: () -> Unit,
    onMultiSelectModeChanged: ((Boolean) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isActionInProgress by viewModel.isActionInProgress.collectAsStateWithLifecycle()
    val showDeleteConfirm by viewModel.showDeleteConfirm.collectAsStateWithLifecycle()
    val destinationMode by viewModel.destinationSelectionMode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showDestinationDialog by rememberSaveable { mutableStateOf(false) }
    var destinationFolders by remember { mutableStateOf<List<PhotoFolder>>(emptyList()) }

    // 전역 selectionMap이 변경될 때마다 아카이브 업데이트
    LaunchedEffect(globalSelectionMap) {
        viewModel.loadArchive(globalSelectionMap = globalSelectionMap)
    }

    // Snackbar 메시지 수집
    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // 공유 이벤트 수집
    val shareTitle = stringResource(R.string.bulk_action_share)
    LaunchedEffect(Unit) {
        viewModel.shareEvents.collectLatest { uris ->
            if (uris.isEmpty()) return@collectLatest
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, shareTitle))
        }
    }

    // 저장소 접근 요청 수집
    val storageAccessLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onStorageAccessResult(result.resultCode == Activity.RESULT_OK)
        }
    LaunchedEffect(Unit) {
        viewModel.storageAccessRequests.collectLatest { intentSender ->
            storageAccessLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    // 목적지 선택 모드 변경 시 폴더 목록 로드 및 다이얼로그 표시
    LaunchedEffect(destinationMode) {
        if (destinationMode != null) {
            destinationFolders = viewModel.loadFolders()
            showDestinationDialog = true
        }
    }

    // 다중 선택 모드 상태 전달
    val isMultiSelectMode = (uiState as? ArchiveUiState.ArchiveReady)?.isMultiSelectMode ?: false
    LaunchedEffect(isMultiSelectMode) {
        onMultiSelectModeChanged?.invoke(isMultiSelectMode)
    }

    // 다중 선택 모드에서 Back 버튼 처리
    BackHandler(enabled = isMultiSelectMode) {
        viewModel.exitMultiSelectMode()
    }

    // 전체 선택 상태 계산
    val selectAllState = remember(uiState) {
        val state = uiState as? ArchiveUiState.ArchiveReady ?: return@remember ToggleableState.Off
        val allIds = state.allPhotoIds
        when {
            allIds.isEmpty() -> ToggleableState.Off
            state.selectedIds.containsAll(allIds) -> ToggleableState.On
            state.selectedIds.isEmpty() -> ToggleableState.Off
            else -> ToggleableState.Indeterminate
        }
    }

    val selectedCount = (uiState as? ArchiveUiState.ArchiveReady)?.selectedCount ?: 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val archiveReady = uiState as? ArchiveUiState.ArchiveReady
            ArchiveTopBar(
                hasAcceptedPhotos = archiveReady?.totalPhotoCount?.let { it > 0 } ?: false,
                isMultiSelectMode = isMultiSelectMode,
                selectedCount = archiveReady?.selectedCount ?: 0,
                selectAllState = selectAllState,
                onCancelSelection = { viewModel.exitMultiSelectMode() },
                onSelectAllToggle = { viewModel.toggleSelectAll() }
            )
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
                is ArchiveUiState.LoadingArchive -> {
                    CircularProgressIndicator()
                }
                is ArchiveUiState.ArchiveReady -> {
                    if (state.folderSections.isEmpty()) {
                        ArchiveEmptyScreen(
                            onNavigateToOrganize = onNavigateToOrganize
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.folderSections,
                                key = { it.sectionId }
                            ) { section ->
                                ArchiveSection(
                                    folderName = section.folderName,
                                    photos = section.photos,
                                    selectedIds = state.selectedIds,
                                    isMultiSelectMode = state.isMultiSelectMode,
                                    onPhotoClick = { photo ->
                                        val bucketId = photo.bucketId ?: section.bucketId
                                        if (bucketId == null) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("사진 폴더 정보를 찾을 수 없어요.")
                                            }
                                        } else {
                                            onNavigateToPhotoDetail(
                                                bucketId,
                                                photo.id,
                                                globalSelectionMap,
                                                true,
                                                emptyMap(),
                                                section.photos.map { it.id }
                                            )
                                        }
                                    },
                                    onToggleSelection = { photoId ->
                                        viewModel.toggleSelection(photoId)
                                    }
                                )
                            }
                        }
                    }
                }
                is ArchiveUiState.EmptyArchive -> {
                    ArchiveEmptyScreen(
                        onNavigateToOrganize = onNavigateToOrganize
                    )
                }
            }
        }

        // 삭제 확인 다이얼로그
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
                        Text(text = stringResource(R.string.delete_confirm_button))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissDeleteConfirmation() },
                        enabled = !isActionInProgress
                    ) {
                        Text(text = stringResource(R.string.delete_confirm_cancel))
                    }
                },
                text = {
                    Text(
                        text = stringResource(R.string.delete_confirm_message, selectedCount)
                    )
                }
            )
        }

        // 목적지 폴더 선택 다이얼로그
        if (showDestinationDialog) {
            val modeLabel = when (destinationMode) {
                is ArchiveViewModel.DestinationSelectionMode.Move -> "이동"
                is ArchiveViewModel.DestinationSelectionMode.Copy -> "복사"
                null -> ""
            }
            AlertDialog(
                onDismissRequest = {
                    showDestinationDialog = false
                    viewModel.cancelDestinationSelection()
                },
                title = { Text(text = "폴더 선택 ($modeLabel)") },
                text = {
                    if (destinationFolders.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn {
                            items(destinationFolders) { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showDestinationDialog = false
                                            when (destinationMode) {
                                                is ArchiveViewModel.DestinationSelectionMode.Move ->
                                                    viewModel.moveToDestination(folder.relativePath)
                                                is ArchiveViewModel.DestinationSelectionMode.Copy ->
                                                    viewModel.copyToDestination(folder.relativePath)
                                                null -> {}
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDestinationDialog = false
                            viewModel.cancelDestinationSelection()
                        }
                    ) {
                        Text(text = stringResource(R.string.delete_confirm_cancel))
                    }
                }
            )
        }
    }
}
