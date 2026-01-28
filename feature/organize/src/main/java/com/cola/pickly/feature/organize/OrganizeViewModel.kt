package com.cola.pickly.feature.organize

import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.domain.refresh.PhotoDataRefreshNotifier
import com.cola.pickly.core.domain.refresh.RefreshReason
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoFolder
import com.cola.pickly.feature.organize.domain.usecase.MoveSelectedPhotosUseCase
import com.cola.pickly.feature.organize.domain.usecase.ShareSelectedPhotosUseCase
import com.cola.pickly.feature.organize.domain.usecase.CopySelectedPhotosUseCase
import com.cola.pickly.feature.organize.domain.usecase.SoftDeleteSelectedPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrganizeViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val photoDataRefreshNotifier: PhotoDataRefreshNotifier,
    private val shareSelectedPhotosUseCase: ShareSelectedPhotosUseCase,
    private val moveSelectedPhotosUseCase: MoveSelectedPhotosUseCase,
    private val copySelectedPhotosUseCase: CopySelectedPhotosUseCase,
    private val softDeleteSelectedPhotosUseCase: SoftDeleteSelectedPhotosUseCase
) : ViewModel() {

    private fun logD(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    private fun logE(message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) Log.e(TAG, message, throwable) else Log.e(TAG, message)
        }
    }

    /**
     * 전역 selectionMap (모든 폴더의 사진 선택 상태 관리)
     * 폴더를 변경해도 이 맵은 유지되어 선택 상태가 보존됩니다.
     */
    private val _globalSelectionMap = MutableStateFlow<Map<Long, PhotoSelectionState>>(emptyMap())
    val globalSelectionMap: StateFlow<Map<Long, PhotoSelectionState>> = _globalSelectionMap.asStateFlow()

    private val _uiState = MutableStateFlow<OrganizeUiState>(OrganizeUiState.NoFolderSelected)
    val uiState: StateFlow<OrganizeUiState> = _uiState.asStateFlow()

    private val _shareEvents = MutableSharedFlow<List<Uri>>(extraBufferCapacity = 1)
    val shareEvents: SharedFlow<List<Uri>> = _shareEvents

    private val _storageAccessRequests = MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val storageAccessRequests: SharedFlow<IntentSender> = _storageAccessRequests

    private val _moveStorageAccessRequests = MutableSharedFlow<IntentSender>(extraBufferCapacity = 1)
    val moveStorageAccessRequests: SharedFlow<IntentSender> = _moveStorageAccessRequests

    private val _showDeleteConfirm = MutableStateFlow(false)
    val showDeleteConfirm: StateFlow<Boolean> = _showDeleteConfirm.asStateFlow()

    private val _destinationSelectionMode = MutableStateFlow<DestinationSelectionMode?>(null)
    val destinationSelectionMode: StateFlow<DestinationSelectionMode?> = _destinationSelectionMode.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages

    private val _isActionInProgress = MutableStateFlow(false)
    val isActionInProgress: StateFlow<Boolean> = _isActionInProgress.asStateFlow()

    private var selectedFolder: SelectedFolder? = null

    private var pendingAction: PendingAction? = null

    /**
     * (공식 API) 폴더 선택 = 선택 상태 저장 + 해당 폴더 로드.
     * UI는 여기만 호출하면 됩니다.
     */
    fun selectFolder(folderId: String, folderName: String) {
        selectedFolder = SelectedFolder(folderId = folderId, folderName = folderName)
        viewModelScope.launch { loadFolder(folderId = folderId, folderName = folderName) }
    }

    /**
     * (호환) 기존 호출부가 남아있어도 동작하도록 유지합니다.
     * 점진적으로 UI에서는 selectFolder()만 사용하도록 정리합니다.
     */
    fun updateSelectedFolder(folderId: String, folderName: String) {
        selectFolder(folderId = folderId, folderName = folderName)
    }

    /**
     * (공식 API) 현재 선택된 폴더를 다시 로드합니다.
     * 이동/삭제처럼 \"원본 폴더의 사진 개수\"가 바뀌는 액션 완료 시점에만 호출합니다.
     */
    fun refreshCurrentFolder() {
        val current = selectedFolder ?: return
        viewModelScope.launch { loadFolder(folderId = current.folderId, folderName = current.folderName) }
    }

    private suspend fun loadFolder(folderId: String, folderName: String) {
        _uiState.value = OrganizeUiState.Loading
        try {
            // 선택된 폴더의 사진 목록 로드 (Bucket ID 기반)
            val photos = photoRepository.getPhotosByBucketId(folderId)

            if (photos.isEmpty()) {
                _uiState.value = OrganizeUiState.EmptyFolder(
                    folderId = folderId,
                    folderName = folderName
                )
            } else {
                // 전역 selectionMap에서 현재 폴더의 사진들에 대한 상태 복원
                val currentFolderSelectionMap = photos
                    .mapNotNull { photo ->
                        _globalSelectionMap.value[photo.id]?.let { state ->
                            photo.id to state
                        }
                    }
                    .toMap()

                _uiState.value = OrganizeUiState.GridReady(
                    folderId = folderId,
                    folderName = folderName,
                    photos = photos,
                    // refresh 시에는 선택을 유지하지 않습니다(멀티선택 종료 정책)
                    selectedIds = emptySet(),
                    selectionMap = currentFolderSelectionMap
                )
            }
        } catch (e: Exception) {
            // 에러 발생 시 처리
            _uiState.value = OrganizeUiState.NoFolderSelected
        }
    }

    // 폴더 선택 취소 테스트용
    fun resetToEmpty() {
        _uiState.value = OrganizeUiState.NoFolderSelected
    }

    /**
     * 전체 선택 토글
     *
     * Wireframe.md S-02 참고:
     * - Tri-state 체크박스로 동작
     * - 모두 선택되어 있으면 → 전체 해제
     * - 하나라도 선택 안 되어 있으면 → 전체 선택
     */
    fun toggleSelectAll() {
        val currentState = _uiState.value
        if (currentState !is OrganizeUiState.GridReady) return

        val allPhotoIds = currentState.photos.map { it.id }.toSet()

        // 모두 선택되어 있으면 → 전체 해제
        // 하나라도 선택 안 되어 있으면 → 전체 선택
        val newSelectedIds = if (currentState.selectedIds.containsAll(allPhotoIds)) {
            currentState.selectedIds - allPhotoIds
        } else {
            currentState.selectedIds + allPhotoIds
        }

        _uiState.value = currentState.copy(selectedIds = newSelectedIds)
    }

    /**
     * 채택 사진 선택 토글
     *
     * Wireframe.md S-02 참고:
     * - 채택 사진이 모두 선택되어 있으면 → 선택 해제
     * - 하나라도 선택 안 되어 있으면 → 모두 선택
     */
    fun toggleAcceptedSelection() {
        val currentState = _uiState.value
        if (currentState !is OrganizeUiState.GridReady) return

        val acceptedPhotoIds = currentState.selectionMap
            .filter { it.value == PhotoSelectionState.Selected }
            .keys
            .toSet()

        val newSelectedIds = if (currentState.selectedIds.containsAll(acceptedPhotoIds)) {
            currentState.selectedIds - acceptedPhotoIds  // OFF: 제거
        } else {
            currentState.selectedIds + acceptedPhotoIds  // ON: 추가
        }

        _uiState.value = currentState.copy(selectedIds = newSelectedIds)
    }

    /**
     * 제외 사진 선택 토글
     *
     * Wireframe.md S-02 참고:
     * - 제외 사진이 모두 선택되어 있으면 → 선택 해제
     * - 하나라도 선택 안 되어 있으면 → 모두 선택
     */
    fun toggleRejectedSelection() {
        val currentState = _uiState.value
        if (currentState !is OrganizeUiState.GridReady) return

        val rejectedPhotoIds = currentState.selectionMap
            .filter { it.value == PhotoSelectionState.Rejected }
            .keys
            .toSet()

        val newSelectedIds = if (currentState.selectedIds.containsAll(rejectedPhotoIds)) {
            currentState.selectedIds - rejectedPhotoIds
        } else {
            currentState.selectedIds + rejectedPhotoIds
        }

        _uiState.value = currentState.copy(selectedIds = newSelectedIds)
    }
    fun toggleSelection(photoId: Long) {
        val currentState = _uiState.value
        if (currentState is OrganizeUiState.GridReady) {
            val newSelection = if (currentState.selectedIds.contains(photoId)) {
                currentState.selectedIds - photoId
            } else {
                currentState.selectedIds + photoId
            }
            _uiState.value = currentState.copy(selectedIds = newSelection)
        }
    }

    fun applySelectionUpdates(updates: Map<Long, PhotoSelectionState>) {
        val currentState = _uiState.value
        if (currentState is OrganizeUiState.GridReady) {
            // 전역 selectionMap 업데이트 (모든 폴더의 사진 상태 관리)
            val newGlobalMap = _globalSelectionMap.value.toMutableMap()
            updates.forEach { (id, state) ->
                if (state == PhotoSelectionState.None) {
                    newGlobalMap.remove(id)
                } else {
                    newGlobalMap[id] = state
                }
            }
            _globalSelectionMap.value = newGlobalMap
            
            // UI 상태 업데이트 (현재 폴더의 사진들만)
            val newSelectionMap = currentState.selectionMap.toMutableMap()
            updates.forEach { (id, state) ->
                // 현재 폴더의 사진인 경우에만 UI 상태 업데이트
                if (currentState.photos.any { it.id == id }) {
                    if (state == PhotoSelectionState.None) {
                        newSelectionMap.remove(id)
                    } else {
                        newSelectionMap[id] = state
                    }
                }
            }
            
            // 변화가 있을 때만 업데이트
            if (newSelectionMap != currentState.selectionMap) {
                _uiState.value = currentState.copy(selectionMap = newSelectionMap)
            }
        }
    }

    /**
     * Multi Select Mode 종료
     * 
     * Wireframe.md S-02 참고:
     * - 모든 선택 상태 해제
     * - Normal Mode로 복귀
     * - Top Bar를 Normal Mode UI로 전환
     * - Bulk Action Bar 숨김
     */
    fun exitMultiSelectMode() {
        val currentState = _uiState.value
        if (currentState is OrganizeUiState.GridReady) {
            _uiState.value = currentState.copy(
                selectedIds = emptySet()
            )
        }
    }

    /**
     * 선택된 사진 공유
     */
    fun shareSelectedPhotos() {
        val currentState = _uiState.value
        if (_isActionInProgress.value) return
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    _isActionInProgress.value = true
                    val uris = shareSelectedPhotosUseCase(currentState.selectedIds.toList())
                    if (uris.isNotEmpty()) {
                        _shareEvents.emit(uris)
                        _snackbarMessages.emit("공유 준비가 완료되었어요.")
                    }
                } catch (e: Exception) {
                    handleActionError(e, "공유 준비에 실패했어요.")
                } finally {
                    _isActionInProgress.value = false
                }
            }
        }
    }

    /**
     * 이동 모드로 목적지 선택 시작
     */
    fun requestDestinationForMove() {
        val currentState = _uiState.value
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            _destinationSelectionMode.value = DestinationSelectionMode.Move
        }
    }

    /**
     * 복사 모드로 목적지 선택 시작
     */
    fun requestDestinationForCopy() {
        val currentState = _uiState.value
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            _destinationSelectionMode.value = DestinationSelectionMode.Copy
        }
    }

    /**
     * 목적지 선택 취소
     */
    fun cancelDestinationSelection() {
        _destinationSelectionMode.value = null
    }

    /**
     * 선택한 폴더로 이동 실행
     */
    fun moveToDestination(folder: PhotoFolder) {
        val currentState = _uiState.value
        if (_isActionInProgress.value) return
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            _destinationSelectionMode.value = null
            viewModelScope.launch {
                val photoIds = currentState.selectedIds.toList()
                runMove(photoIds, folder.relativePath)
            }
        }
    }

    /**
     * 선택한 폴더로 복사 실행
     */
    fun copyToDestination(folder: PhotoFolder) {
        val currentState = _uiState.value
        if (_isActionInProgress.value) return
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            _destinationSelectionMode.value = null
            viewModelScope.launch {
                val photoIds = currentState.selectedIds.toList()
                runCopy(photoIds, folder.relativePath)
            }
        }
    }

    /**
     * 새 폴더 생성 및 작업 실행
     */
    fun createFolderAndExecute(folderName: String) {
        val currentState = _uiState.value
        if (_isActionInProgress.value) return
        if (currentState !is OrganizeUiState.GridReady || currentState.selectedIds.isEmpty()) return

        val mode = _destinationSelectionMode.value ?: return
        _destinationSelectionMode.value = null

        viewModelScope.launch {
            val photoIds = currentState.selectedIds.toList()
            val destinationPath = "DCIM/$folderName"

            when (mode) {
                is DestinationSelectionMode.Move -> runMove(photoIds, destinationPath)
                is DestinationSelectionMode.Copy -> runCopy(photoIds, destinationPath)
            }
        }
    }


    /**
     * 선택된 사진 삭제 (휴지통 이동)
     */
    fun requestDeleteConfirmation() {
        val currentState = _uiState.value
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            _showDeleteConfirm.value = true
        }
    }

    fun dismissDeleteConfirmation() {
        _showDeleteConfirm.value = false
    }

    fun deleteSelectedPhotos() {
        val currentState = _uiState.value
        if (_isActionInProgress.value) return
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                _showDeleteConfirm.value = false
                val photoIds = currentState.selectedIds.toList()
                logD("deleteSelectedPhotos selected=${photoIds.size} ids(sample)=${photoIds.take(3)}")

                val intentSender = runCatching {
                    softDeleteSelectedPhotosUseCase.createWriteRequestIntentSender(photoIds)
                }.getOrNull()

                if (intentSender != null) {
                    logD("deleteSelectedPhotos emit storageAccessRequests intentSender=$intentSender")
                    pendingAction = PendingAction.SoftDelete(photoIds)
                    _storageAccessRequests.emit(intentSender)
                    return@launch
                }

                runSoftDelete(photoIds)
            }
        }
    }

    fun onStorageAccessResult(isApproved: Boolean) {
        val action = pendingAction ?: return
        pendingAction = null
        logD("onStorageAccessResult approved=$isApproved action=$action")

        if (!isApproved) {
            viewModelScope.launch {
                when (action) {
                    is PendingAction.MoveFinalize -> {
                        _snackbarMessages.emit("원본 삭제 승인이 없어 이동이 완료되지 않았어요. Pickly 폴더에는 복사만 완료됐어요.")
                        // 복사 결과를 함께 안내
                        emitReportMessage("복사", action.copiedReport)
                        // 복사는 완료됐을 수 있으므로(Pickly 폴더에 추가), 폴더 목록 갱신 트리거
                        if (action.copiedReport.successCount > 0) {
                            photoDataRefreshNotifier.notify(RefreshReason.CopyCommitted)
                        }
                        exitMultiSelectMode()
                    }
                    is PendingAction.SoftDelete -> {
                        _snackbarMessages.emit("저장소 접근 권한이 필요해요. 설정에서 권한을 허용한 뒤 다시 시도해주세요.")
                    }
                }
            }
            return
        }

        viewModelScope.launch {
            when (action) {
                is PendingAction.MoveFinalize -> {
                    // createDeleteRequest는 시스템이 실제 삭제를 수행합니다.
                    emitReportMessage("이동", action.copiedReport)
                    // 원본 삭제가 커밋되었다고 보고, 폴더 목록 + 현재 폴더 그리드 갱신 트리거
                    if (action.deleteIds.isNotEmpty()) {
                        removeFromGlobalSelection(action.deleteIds)
                        photoDataRefreshNotifier.notify(RefreshReason.MoveCommitted)
                        refreshCurrentFolder()
                    } else if (action.copiedReport.successCount > 0) {
                        // 삭제 대상은 없지만 복사본은 생겼을 수 있으므로 폴더 목록 갱신
                        photoDataRefreshNotifier.notify(RefreshReason.CopyCommitted)
                    }
                    exitMultiSelectMode()
                }
                is PendingAction.SoftDelete -> runSoftDelete(action.photoIds)
            }
        }
    }

    private suspend fun runMove(photoIds: List<Long>, destinationRelativePath: String) {
        try {
            _isActionInProgress.value = true
            val report = moveSelectedPhotosUseCase(photoIds, destinationRelativePath)

            // 복사 성공한 항목만 삭제 승인 요청
            val idsToDelete = report.successIds
            if (idsToDelete.isNotEmpty()) {
                // 목적지 폴더에는 복사가 이미 반영되므로 폴더 목록 갱신 트리거(원본 삭제 전 단계)
                photoDataRefreshNotifier.notify(RefreshReason.CopyCommitted)
                val intentSender = runCatching {
                    moveSelectedPhotosUseCase.createDeleteRequestIntentSender(idsToDelete)
                }.getOrNull()

                if (intentSender != null) {
                    pendingAction = PendingAction.MoveFinalize(
                        copiedReport = report,
                        deleteIds = idsToDelete
                    )
                    _moveStorageAccessRequests.emit(intentSender)
                    return
                }
            }

            // 삭제 승인 요청이 필요 없거나 생성 실패: 이동 결과로 안내
            emitReportMessage("이동", report)
            // 원본은 삭제되지 않았으므로, 폴더 목록만 갱신 트리거(복사 성공분 있을 때)
            if (report.successCount > 0) {
                photoDataRefreshNotifier.notify(RefreshReason.CopyCommitted)
            }
            exitMultiSelectMode()
        } catch (e: Exception) {
            // 이동 실패 케이스만 에러 로그 남김
            logE("moveSelectedPhotos failed size=${photoIds.size}", e)
            handleActionError(e, "이동 중 오류가 발생했어요.")
        } finally {
            _isActionInProgress.value = false
        }
    }

    private suspend fun runCopy(photoIds: List<Long>, destinationRelativePath: String) {
        try {
            _isActionInProgress.value = true
            val report = copySelectedPhotosUseCase(photoIds, destinationRelativePath)
            runCatching {
                android.util.Log.d(
                    "OrganizeViewModel",
                    "복사 완료 success=${report.successCount}, skipped=${report.skippedCount}, failed=${report.failedCount}"
                )
            }
            emitReportMessage("복사", report)
            // 복사 성공분이 있을 때만 폴더 목록 갱신 트리거
            if (report.successCount > 0) {
                photoDataRefreshNotifier.notify(RefreshReason.CopyCommitted)
            }
            exitMultiSelectMode()
        } catch (e: Exception) {
            handleActionError(e, "복사 중 오류가 발생했어요.")
        } finally {
            _isActionInProgress.value = false
        }
    }

    private suspend fun runSoftDelete(photoIds: List<Long>) {
        try {
            _isActionInProgress.value = true
            val report = softDeleteSelectedPhotosUseCase(photoIds)
            runCatching {
                android.util.Log.d(
                    "OrganizeViewModel",
                    "삭제 완료 success=${report.successCount}, failed=${report.failedCount}"
                )
            }
            emitReportMessage("삭제", report)
            // 실제 삭제/휴지통 이동 성공분이 있을 때만 갱신 트리거 + 현재 폴더 리프레시
            if (report.successCount > 0) {
                photoDataRefreshNotifier.notify(RefreshReason.DeleteCommitted)
                refreshCurrentFolder()
            }
            exitMultiSelectMode()
        } catch (e: Exception) {
            handleActionError(e, "삭제 중 오류가 발생했어요.")
        } finally {
            _isActionInProgress.value = false
        }
    }

    private fun removeFromGlobalSelection(photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        val newGlobalMap = _globalSelectionMap.value.toMutableMap()
        photoIds.forEach { id -> newGlobalMap.remove(id) }
        _globalSelectionMap.value = newGlobalMap
    }

    private suspend fun emitReportMessage(actionType: String, report: com.cola.pickly.core.data.photo.PhotoActionReport) {
        // 권한 관련 오류가 있는지 확인
        val hasPermissionError = report.errors.any {
            it.contains("권한") || it.contains("permission", ignoreCase = true) ||
            it.contains("SecurityException", ignoreCase = true)
        }

        if (hasPermissionError && report.failedCount > 0) {
            // 권한 오류가 있는 경우 사용자 친화적인 메시지 표시
            _snackbarMessages.emit("저장소 접근 권한이 필요해요. 설정에서 권한을 허용한 뒤 다시 시도해주세요.")
            return
        }

        // 전체 실패 판정: failedCount > 0 또는 successCount == 0
        val isFailure = report.failedCount > 0 || report.successCount == 0

        val message = when (actionType) {
            "이동" -> if (isFailure) {
                "이동을 완료하지 못했어요. 다시 시도해주세요."
            } else {
                "사진 ${report.successCount}개 이동을 완료했어요."
            }
            "복사" -> if (isFailure) {
                "복사를 완료하지 못했어요. 다시 시도해주세요."
            } else {
                "사진 ${report.successCount}개 복사를 완료했어요."
            }
            "삭제" -> if (isFailure) {
                "삭제를 완료하지 못했어요. 다시 시도해주세요."
            } else {
                "사진 ${report.successCount}개를 휴지통으로 이동했어요."
            }
            else -> "$actionType 완료: ${report.successCount} 성공"
        }

        _snackbarMessages.emit(message)
    }

    private suspend fun handleActionError(e: Exception, fallback: String) {
        val message = when {
            e is SecurityException -> {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    "저장소 쓰기 권한이 필요해요. 앱을 재시작하여 권한을 허용해주세요."
                } else {
                    "저장소 접근 권한이 필요해요. 설정에서 권한을 허용한 뒤 다시 시도해주세요."
                }
            }
            e.message?.contains("permission", ignoreCase = true) == true -> {
                "저장소 접근 권한이 필요해요. 설정에서 권한을 허용한 뒤 다시 시도해주세요."
            }
            e.message?.contains("SAF", ignoreCase = true) == true -> {
                "저장소 접근에 실패했어요. 권한을 확인한 뒤 다시 시도해주세요."
            }
            else -> {
                e.message ?: fallback
            }
        }
        _snackbarMessages.emit(message)
    }

    private sealed interface PendingAction {
        data class MoveFinalize(
            val copiedReport: com.cola.pickly.core.data.photo.PhotoActionReport,
            val deleteIds: List<Long>
        ) : PendingAction
        data class SoftDelete(val photoIds: List<Long>) : PendingAction
    }

    private data class SelectedFolder(
        val folderId: String,
        val folderName: String
    )

    sealed interface DestinationSelectionMode {
        data object Move : DestinationSelectionMode
        data object Copy : DestinationSelectionMode
    }

    private companion object {
        private const val TAG = "OrganizeViewModel"
    }
}
