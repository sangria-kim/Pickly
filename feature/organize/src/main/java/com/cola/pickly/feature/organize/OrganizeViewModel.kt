package com.cola.pickly.feature.organize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.model.WeeklyPhoto
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.feature.organize.components.FilterOption
import com.cola.pickly.core.model.PhotoSelectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrganizeViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    /**
     * 전역 selectionMap (모든 폴더의 사진 선택 상태 관리)
     * 폴더를 변경해도 이 맵은 유지되어 선택 상태가 보존됩니다.
     */
    private val _globalSelectionMap = MutableStateFlow<Map<Long, PhotoSelectionState>>(emptyMap())
    val globalSelectionMap: StateFlow<Map<Long, PhotoSelectionState>> = _globalSelectionMap.asStateFlow()

    private val _uiState = MutableStateFlow<OrganizeUiState>(OrganizeUiState.NoFolderSelected)
    val uiState: StateFlow<OrganizeUiState> = _uiState.asStateFlow()

    fun updateSelectedFolder(folderId: String, folderName: String) {
        viewModelScope.launch {
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
                        selectionMap = currentFolderSelectionMap
                    )
                }
            } catch (e: Exception) {
                // 에러 발생 시 처리
                _uiState.value = OrganizeUiState.NoFolderSelected
            }
        }
    }

    // 폴더 선택 취소 테스트용
    fun resetToEmpty() {
        _uiState.value = OrganizeUiState.NoFolderSelected
    }

    /**
     * 필터 변경 핸들러
     * 
     * Wireframe.md S-02 참고:
     * - 필터 선택은 Multi Select Mode 진입 트리거
     * - 필터 조건에 맞는 사진들을 selectedIds에 추가 (기존 선택 유지)
     * - 필터는 토글이 아닌 추가 선택 방식
     */
    fun onFilterChanged(option: FilterOption) {
        val currentState = _uiState.value
        if (currentState !is OrganizeUiState.GridReady) return

        val photosToSelect = when (option) {
            FilterOption.ALL -> {
                // 전체 사진 선택
                currentState.photos.map { it.id }.toSet()
            }
            FilterOption.SELECTED -> {
                // 채택된 사진 선택 (selectionMap에서 Selected 상태인 사진)
                currentState.selectionMap
                    .filter { it.value == PhotoSelectionState.Selected }
                    .keys
                    .toSet()
            }
            FilterOption.NOT_SELECTED -> {
                // 제외된 사진 선택 (selectionMap에서 Rejected 상태인 사진)
                currentState.selectionMap
                    .filter { it.value == PhotoSelectionState.Rejected }
                    .keys
                    .toSet()
            }
        }

        // 기존 선택과 합집합 (중복 제거)
        val newSelectedIds = currentState.selectedIds union photosToSelect

        // 선택된 사진이 1장 이상이면 Multi Select Mode 활성화
        if (newSelectedIds.isNotEmpty()) {
            _uiState.value = currentState.copy(selectedIds = newSelectedIds)
        }
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
     * V1에서는 기본 동작만 구현 (실제 기능은 향후 확장)
     */
    fun shareSelectedPhotos() {
        val currentState = _uiState.value
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            // TODO: 실제 공유 기능 구현
            // 현재는 선택된 사진 ID만 로그로 출력
            android.util.Log.d("OrganizeViewModel", "공유: ${currentState.selectedIds.size}장 선택됨")
        }
    }

    /**
     * 선택된 사진 이동
     * V1에서는 기본 동작만 구현 (실제 기능은 향후 확장)
     */
    fun moveSelectedPhotos() {
        val currentState = _uiState.value
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            // TODO: 실제 이동 기능 구현
            android.util.Log.d("OrganizeViewModel", "이동: ${currentState.selectedIds.size}장 선택됨")
        }
    }

    /**
     * 선택된 사진 복사
     * V1에서는 기본 동작만 구현 (실제 기능은 향후 확장)
     */
    fun copySelectedPhotos() {
        val currentState = _uiState.value
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            // TODO: 실제 복사 기능 구현
            android.util.Log.d("OrganizeViewModel", "복사: ${currentState.selectedIds.size}장 선택됨")
        }
    }

    /**
     * 선택된 사진 삭제
     * V1에서는 기본 동작만 구현 (실제 기능은 향후 확장)
     * 삭제 시 확인 다이얼로그 필요 (Wireframe.md 참고)
     */
    fun deleteSelectedPhotos() {
        val currentState = _uiState.value
        if (currentState is OrganizeUiState.GridReady && currentState.selectedIds.isNotEmpty()) {
            // TODO: 실제 삭제 기능 구현 (확인 다이얼로그 포함)
            android.util.Log.d("OrganizeViewModel", "삭제: ${currentState.selectedIds.size}장 선택됨")
        }
    }
}
