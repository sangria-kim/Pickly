package com.cola.pickly.feature.archive

import android.content.IntentSender
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoDisplayOrderComparator
import com.cola.pickly.core.model.SortOrder
import com.cola.pickly.core.model.photoComparatorFor
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.domain.refresh.PhotoDataRefreshNotifier
import com.cola.pickly.core.domain.refresh.RefreshReason
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.data.photo.PhotoActionReport
import com.cola.pickly.core.data.usecase.ShareSelectedPhotosUseCase
import com.cola.pickly.core.data.usecase.MoveSelectedPhotosUseCase
import com.cola.pickly.core.data.usecase.CopySelectedPhotosUseCase
import com.cola.pickly.core.data.usecase.SoftDeleteSelectedPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val photoDataRefreshNotifier: PhotoDataRefreshNotifier,
    private val shareSelectedPhotosUseCase: ShareSelectedPhotosUseCase,
    private val moveSelectedPhotosUseCase: MoveSelectedPhotosUseCase,
    private val copySelectedPhotosUseCase: CopySelectedPhotosUseCase,
    private val softDeleteSelectedPhotosUseCase: SoftDeleteSelectedPhotosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ArchiveUiState>(ArchiveUiState.LoadingArchive)
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    private val _isActionInProgress = MutableStateFlow(false)
    val isActionInProgress: StateFlow<Boolean> = _isActionInProgress.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages

    private val _shareEvents = MutableSharedFlow<List<Uri>>(extraBufferCapacity = 1)
    val shareEvents: SharedFlow<List<Uri>> = _shareEvents

    private val _storageAccessRequests = MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val storageAccessRequests: SharedFlow<IntentSender> = _storageAccessRequests

    private val _showDeleteConfirm = MutableStateFlow(false)
    val showDeleteConfirm: StateFlow<Boolean> = _showDeleteConfirm.asStateFlow()

    private val _destinationSelectionMode = MutableStateFlow<DestinationSelectionMode?>(null)
    val destinationSelectionMode: StateFlow<DestinationSelectionMode?> = _destinationSelectionMode.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private var pendingAction: PendingAction? = null

    private var lastGlobalSelectionMap: Map<Long, PhotoSelectionState> = emptyMap()

    fun loadArchive(
        globalSelectionMap: Map<Long, PhotoSelectionState>
    ) {
        lastGlobalSelectionMap = globalSelectionMap
        viewModelScope.launch {
            _uiState.value = ArchiveUiState.LoadingArchive

            try {
                val (allPhotos, folders) = coroutineScope {
                    val photos = async {
                        photoRepository.getAllPhotos().sortedWith(photoComparatorFor(_sortOrder.value))
                    }
                    val folderList = async {
                        photoRepository.getFolders()
                    }
                    photos.await() to folderList.await()
                }

                val selectedIds = globalSelectionMap
                    .filterValues { it == PhotoSelectionState.Selected }
                    .keys
                val acceptedPhotos = allPhotos.filter { photo -> selectedIds.contains(photo.id) }

                if (acceptedPhotos.isEmpty()) {
                    _uiState.value = ArchiveUiState.EmptyArchive
                    return@launch
                }

                val folderNameMap = folders.associate { folder ->
                    folder.id to folder.name
                }
                val sections = acceptedPhotos
                    .groupBy { photo ->
                        photo.bucketId ?: "unknown:${extractFolderName(photo.filePath)}"
                    }
                    .map { (groupKey, photos) ->
                        val bucketId = photos.firstOrNull()?.bucketId
                        val folderName = when {
                            bucketId != null -> folderNameMap[bucketId] ?: extractFolderName(photos.first().filePath)
                            else -> extractFolderName(photos.first().filePath)
                        }
                        ArchiveFolderSection(
                            sectionId = bucketId ?: groupKey,
                            bucketId = bucketId,
                            folderName = folderName,
                            photos = photos
                        )
                    }
                    .sortedWith(
                        compareBy<ArchiveFolderSection> { it.folderName.lowercase() }
                            .thenBy { it.sectionId }
                    )

                _uiState.value = ArchiveUiState.ArchiveReady(
                    folderSections = sections
                )
            } catch (e: Exception) {
                _uiState.value = ArchiveUiState.EmptyArchive
            }
        }
    }

    fun toggleSortOrder() {
        val newOrder = if (_sortOrder.value == SortOrder.DESCENDING) SortOrder.ASCENDING else SortOrder.DESCENDING
        _sortOrder.value = newOrder
        _uiState.update { state ->
            if (state is ArchiveUiState.ArchiveReady) {
                val comparator = photoComparatorFor(newOrder)
                state.copy(
                    folderSections = state.folderSections.map { section ->
                        section.copy(photos = section.photos.sortedWith(comparator))
                    }
                )
            } else state
        }
    }

    fun toggleSelection(photoId: Long) {
        _uiState.update { state ->
            if (state is ArchiveUiState.ArchiveReady) {
                val newSelection = if (state.selectedIds.contains(photoId)) {
                    state.selectedIds - photoId
                } else {
                    state.selectedIds + photoId
                }
                state.copy(selectedIds = newSelection)
            } else state
        }
    }

    fun toggleSelectAll() {
        _uiState.update { state ->
            if (state is ArchiveUiState.ArchiveReady) {
                val allIds = state.allPhotoIds
                val newSelectedIds = if (state.selectedIds.containsAll(allIds)) {
                    state.selectedIds - allIds
                } else {
                    state.selectedIds + allIds
                }
                state.copy(selectedIds = newSelectedIds)
            } else state
        }
    }

    fun toggleSelectAllInFolder(sectionId: String) {
        _uiState.update { state ->
            if (state is ArchiveUiState.ArchiveReady) {
                val folderIds = state.folderSections
                    .find { it.sectionId == sectionId }
                    ?.photos?.map { it.id }?.toSet() ?: return@update state
                val newSelectedIds = if (state.selectedIds.containsAll(folderIds)) {
                    state.selectedIds - folderIds
                } else {
                    state.selectedIds + folderIds
                }
                state.copy(selectedIds = newSelectedIds)
            } else state
        }
    }

    fun exitMultiSelectMode() {
        _uiState.update { state ->
            if (state is ArchiveUiState.ArchiveReady) {
                state.copy(selectedIds = emptySet())
            } else state
        }
    }

    // --- Bulk Actions ---

    fun shareSelectedPhotos() {
        val currentState = _uiState.value
        if (_isActionInProgress.value) return
        if (currentState !is ArchiveUiState.ArchiveReady || currentState.selectedIds.isEmpty()) return

        viewModelScope.launch {
            try {
                _isActionInProgress.value = true
                val uris = shareSelectedPhotosUseCase(currentState.selectedIds.toList())
                if (uris.isNotEmpty()) {
                    _shareEvents.emit(uris)
                    _snackbarMessages.emit("공유 준비가 완료되었어요.")
                }
            } catch (e: Exception) {
                _snackbarMessages.emit("공유 준비에 실패했어요.")
            } finally {
                _isActionInProgress.value = false
            }
        }
    }

    fun requestDestinationForMove() {
        val currentState = _uiState.value
        if (currentState is ArchiveUiState.ArchiveReady && currentState.selectedIds.isNotEmpty()) {
            _destinationSelectionMode.value = DestinationSelectionMode.Move
        }
    }

    fun requestDestinationForCopy() {
        val currentState = _uiState.value
        if (currentState is ArchiveUiState.ArchiveReady && currentState.selectedIds.isNotEmpty()) {
            _destinationSelectionMode.value = DestinationSelectionMode.Copy
        }
    }

    fun cancelDestinationSelection() {
        _destinationSelectionMode.value = null
    }

    fun moveToDestination(destinationRelativePath: String) {
        val currentState = _uiState.value
        if (_isActionInProgress.value) return
        if (currentState !is ArchiveUiState.ArchiveReady || currentState.selectedIds.isEmpty()) return
        _destinationSelectionMode.value = null

        viewModelScope.launch {
            try {
                _isActionInProgress.value = true
                val photoIds = currentState.selectedIds.toList()
                val report = moveSelectedPhotosUseCase(photoIds, destinationRelativePath)

                val idsToDelete = report.successIds
                if (idsToDelete.isNotEmpty()) {
                    photoDataRefreshNotifier.notify(RefreshReason.CopyCommitted)
                    val intentSender = runCatching {
                        moveSelectedPhotosUseCase.createDeleteRequestIntentSender(idsToDelete)
                    }.getOrNull()

                    if (intentSender != null) {
                        pendingAction = PendingAction.MoveFinalize(
                            copiedReport = report,
                            deleteIds = idsToDelete
                        )
                        _storageAccessRequests.emit(intentSender)
                        return@launch
                    }
                }

                emitReportMessage("이동", report)
                if (report.successCount > 0) {
                    photoDataRefreshNotifier.notify(RefreshReason.MoveCommitted)
                    reloadArchive()
                }
                exitMultiSelectMode()
            } catch (e: Exception) {
                _snackbarMessages.emit("이동 중 오류가 발생했어요.")
            } finally {
                _isActionInProgress.value = false
            }
        }
    }

    fun copyToDestination(destinationRelativePath: String) {
        val currentState = _uiState.value
        if (_isActionInProgress.value) return
        if (currentState !is ArchiveUiState.ArchiveReady || currentState.selectedIds.isEmpty()) return
        _destinationSelectionMode.value = null

        viewModelScope.launch {
            try {
                _isActionInProgress.value = true
                val photoIds = currentState.selectedIds.toList()
                val report = copySelectedPhotosUseCase(photoIds, destinationRelativePath)
                emitReportMessage("복사", report)
                if (report.successCount > 0) {
                    photoDataRefreshNotifier.notify(RefreshReason.CopyCommitted)
                }
                exitMultiSelectMode()
            } catch (e: Exception) {
                _snackbarMessages.emit("복사 중 오류가 발생했어요.")
            } finally {
                _isActionInProgress.value = false
            }
        }
    }

    fun requestDeleteConfirmation() {
        val currentState = _uiState.value
        if (currentState is ArchiveUiState.ArchiveReady && currentState.selectedIds.isNotEmpty()) {
            _showDeleteConfirm.value = true
        }
    }

    fun dismissDeleteConfirmation() {
        _showDeleteConfirm.value = false
    }

    fun deleteSelectedPhotos() {
        val currentState = _uiState.value
        if (_isActionInProgress.value) return
        if (currentState !is ArchiveUiState.ArchiveReady || currentState.selectedIds.isEmpty()) return

        viewModelScope.launch {
            _showDeleteConfirm.value = false
            val photoIds = currentState.selectedIds.toList()

            val intentSender = runCatching {
                softDeleteSelectedPhotosUseCase.createWriteRequestIntentSender(photoIds)
            }.getOrNull()

            if (intentSender != null) {
                pendingAction = PendingAction.SoftDelete(photoIds)
                _storageAccessRequests.emit(intentSender)
                return@launch
            }

            runSoftDelete(photoIds)
        }
    }

    fun onStorageAccessResult(isApproved: Boolean) {
        val action = pendingAction ?: return
        pendingAction = null

        if (!isApproved) {
            viewModelScope.launch {
                when (action) {
                    is PendingAction.MoveFinalize -> {
                        _snackbarMessages.emit("원본 삭제 승인이 없어 이동이 완료되지 않았어요.")
                        if (action.copiedReport.successCount > 0) {
                            photoDataRefreshNotifier.notify(RefreshReason.CopyCommitted)
                        }
                        exitMultiSelectMode()
                    }
                    is PendingAction.SoftDelete -> {
                        _snackbarMessages.emit("저장소 접근 권한이 필요해요.")
                    }
                }
                _isActionInProgress.value = false
            }
            return
        }

        viewModelScope.launch {
            when (action) {
                is PendingAction.MoveFinalize -> {
                    emitReportMessage("이동", action.copiedReport)
                    if (action.deleteIds.isNotEmpty()) {
                        photoDataRefreshNotifier.notify(RefreshReason.MoveCommitted)
                        reloadArchive()
                    }
                    exitMultiSelectMode()
                    _isActionInProgress.value = false
                }
                is PendingAction.SoftDelete -> runSoftDelete(action.photoIds)
            }
        }
    }

    fun createFolderAndExecute(folderName: String) {
        val currentState = _uiState.value
        if (_isActionInProgress.value) return
        if (currentState !is ArchiveUiState.ArchiveReady || currentState.selectedIds.isEmpty()) return

        val mode = _destinationSelectionMode.value ?: return
        _destinationSelectionMode.value = null

        val destinationPath = "DCIM/$folderName"
        when (mode) {
            is DestinationSelectionMode.Move -> moveToDestination(destinationPath)
            is DestinationSelectionMode.Copy -> copyToDestination(destinationPath)
        }
    }

    private suspend fun runSoftDelete(photoIds: List<Long>) {
        try {
            _isActionInProgress.value = true
            val report = softDeleteSelectedPhotosUseCase(photoIds)
            emitReportMessage("삭제", report)
            if (report.successCount > 0) {
                photoDataRefreshNotifier.notify(RefreshReason.DeleteCommitted)
                reloadArchive()
            }
            exitMultiSelectMode()
        } catch (e: Exception) {
            _snackbarMessages.emit("삭제 중 오류가 발생했어요.")
        } finally {
            _isActionInProgress.value = false
        }
    }

    suspend fun loadFolders(): List<com.cola.pickly.core.model.PhotoFolder> {
        return try {
            photoRepository.getFolders()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun reloadArchive() {
        loadArchive(lastGlobalSelectionMap)
    }

    private suspend fun emitReportMessage(actionType: String, report: PhotoActionReport) {
        val isFailure = report.failedCount > 0 || report.successCount == 0
        val message = when (actionType) {
            "이동" -> if (isFailure) "이동을 완료하지 못했어요." else "사진 ${report.successCount}개 이동을 완료했어요."
            "복사" -> if (isFailure) "복사를 완료하지 못했어요." else "사진 ${report.successCount}개 복사를 완료했어요."
            "삭제" -> if (isFailure) "삭제를 완료하지 못했어요." else "사진 ${report.successCount}개를 휴지통으로 이동했어요."
            else -> "$actionType 완료: ${report.successCount} 성공"
        }
        _snackbarMessages.emit(message)
    }

    private fun extractFolderName(filePath: String): String {
        val normalizedPath = filePath.trimEnd('/')
        val folderName = normalizedPath.substringBeforeLast("/", missingDelimiterValue = "")
            .substringAfterLast("/", missingDelimiterValue = "")
        return folderName.ifEmpty { "Unknown" }
    }

    sealed interface DestinationSelectionMode {
        data object Move : DestinationSelectionMode
        data object Copy : DestinationSelectionMode
    }

    private sealed interface PendingAction {
        data class MoveFinalize(
            val copiedReport: PhotoActionReport,
            val deleteIds: List<Long>
        ) : PendingAction
        data class SoftDelete(val photoIds: List<Long>) : PendingAction
    }
}
