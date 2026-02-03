package com.cola.pickly.presentation.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.data.database.PhotoScoreDao
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RejectReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val photoScoreDao: PhotoScoreDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val folderId: String? = savedStateHandle["folderId"]
    private val initialPhotoId: Long? = savedStateHandle["photoId"]
    private val selectedOnly: Boolean = savedStateHandle["selectedOnly"] ?: false
    private val savedStateHandleRef = savedStateHandle
    
    private var initialSelectionMap: Map<Long, PhotoSelectionState> = 
        savedStateHandle.get<Map<Long, PhotoSelectionState>>("initial_selection_map") ?: emptyMap()

    private var initialRejectCandidates: Map<Long, RejectReason> = 
        savedStateHandle.get<Map<Long, RejectReason>>("initial_reject_candidates") ?: emptyMap()

    private val _uiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Loading)
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }
    
    fun initializeViewerData(
        selectionMap: Map<Long, PhotoSelectionState>,
        rejectCandidates: Map<Long, RejectReason>
    ) {
        // savedStateHandle에 저장하여 loadPhotos()에서 읽을 수 있도록 함
        savedStateHandleRef["initial_selection_map"] = selectionMap
        savedStateHandleRef["initial_reject_candidates"] = rejectCandidates
        
        initialSelectionMap = selectionMap
        initialRejectCandidates = rejectCandidates
        
        // 이미 로드된 경우 상태 업데이트
        if (_uiState.value is ViewerUiState.Success) {
            _uiState.update { currentState ->
                if (currentState is ViewerUiState.Success) {
                    val updatedPhotos = currentState.photos.map { photo ->
                        // Apply reject candidates if needed
                        applyRejectCandidate(photo, rejectCandidates)
                    }
                    currentState.copy(
                        photos = updatedPhotos,
                        selectionMap = selectionMap
                    )
                } else {
                    currentState
                }
            }
        }
    }

    private fun loadPhotos() {
        if (folderId.isNullOrBlank()) {
            _uiState.value = ViewerUiState.Error("잘못된 접근입니다. (Folder ID 누락)")
            return
        }

        viewModelScope.launch {
            try {
                // 폴더의 사진 목록 로드 (Bucket ID 기반)
                // TODO: 페이징이나 대량 데이터 처리 고려 필요, 현재는 전체 로드
                val allPhotos = photoRepository.getPhotosByBucketId(folderId)

                // selectionMap 및 rejectCandidates 사용 시점에 최신값 읽기
                val currentSelectionMap = savedStateHandleRef.get<Map<Long, PhotoSelectionState>>("initial_selection_map")
                    ?: initialSelectionMap
                val currentRejectCandidates = savedStateHandleRef.get<Map<Long, RejectReason>>("initial_reject_candidates")
                    ?: initialRejectCandidates

                // 각 Photo의 recommendationScore를 DB에서 로드하고 Reject Candidate 반영
                val photosWithScores = allPhotos.map { photo ->
                    var photoWithScore = photo
                    val scoreEntity = photoScoreDao.getScore(photo.id)
                    
                    if (scoreEntity != null) {
                        photoWithScore = photo.copy(recommendationScore = scoreEntity.score)
                    }
                    
                    applyRejectCandidate(photoWithScore, currentRejectCandidates)
                }

                val filteredPhotos = if (selectedOnly) {
                    val selectedIds = currentSelectionMap
                        .filterValues { it == PhotoSelectionState.Selected }
                        .keys
                    photosWithScores.filter { selectedIds.contains(it.id) }
                } else {
                    photosWithScores
                }
                
                if (filteredPhotos.isEmpty()) {
                    _uiState.value = ViewerUiState.Error("사진이 없는 폴더입니다.")
                } else {
                    // 초기 인덱스 계산
                    val initialIndex = if (initialPhotoId != null) {
                        val index = filteredPhotos.indexOfFirst { it.id == initialPhotoId }
                        if (index != -1) index else 0
                    } else {
                        0
                    }
                    
                    _uiState.value = ViewerUiState.Success(
                        photos = filteredPhotos,
                        initialIndex = initialIndex,
                        selectionMap = if (selectedOnly) {
                            currentSelectionMap.filterKeys { key ->
                                filteredPhotos.any { it.id == key }
                            }
                        } else {
                            currentSelectionMap
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ViewerUiState.Error(e.message ?: "사진을 불러오는 중 에러가 발생했습니다.")
            }
        }
    }

    private fun applyRejectCandidate(photo: com.cola.pickly.core.model.Photo, candidates: Map<Long, RejectReason>): com.cola.pickly.core.model.Photo {
        val candidateReason = candidates[photo.id] ?: return photo
        
        val currentScore = photo.recommendationScore ?: com.cola.pickly.core.model.RecommendationScore.DEFAULT
        return photo.copy(
            recommendationScore = currentScore.copy(rejectReason = candidateReason)
        )
    }

    fun toggleSelection(photoId: Long) {
        updateSelectionState(photoId, PhotoSelectionState.Selected)
    }

    fun toggleRejection(photoId: Long) {
        updateSelectionState(photoId, PhotoSelectionState.Rejected)
    }

    private fun updateSelectionState(photoId: Long, targetState: PhotoSelectionState) {
        _uiState.update { currentState ->
            if (currentState is ViewerUiState.Success) {
                val currentMap = currentState.selectionMap.toMutableMap()
                val currentSelection = currentMap[photoId] ?: PhotoSelectionState.None

                val newState = if (currentSelection == targetState) {
                    PhotoSelectionState.None // 이미 해당 상태면 해제 (토글)
                } else {
                    targetState // 다른 상태면 타겟 상태로 변경 (상호 배타적 동작)
                }

                currentMap[photoId] = newState
                currentState.copy(selectionMap = currentMap)
            } else {
                currentState
            }
        }
    }
}
