package com.cola.pickly.presentation.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.data.database.PhotoScoreDao
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.model.Photo
import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.RecommendationScore
import com.cola.pickly.core.model.RejectReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AutoAdvanceEvent {
    data class Advance(val targetIndex: Int) : AutoAdvanceEvent
    data object AtLastPage : AutoAdvanceEvent
}

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
    private var initialOrderedPhotoIds: List<Long> =
        savedStateHandle.get<List<Long>>("initial_ordered_photo_ids") ?: emptyList()

    private val _uiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Loading)
    val uiState: StateFlow<ViewerUiState> = _uiState.asStateFlow()

    private val _autoAdvanceEvent = MutableSharedFlow<AutoAdvanceEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val autoAdvanceEvent: SharedFlow<AutoAdvanceEvent> = _autoAdvanceEvent.asSharedFlow()

    init {
        loadPhotos()
    }
    
    fun initializeViewerData(
        selectionMap: Map<Long, PhotoSelectionState>,
        rejectCandidates: Map<Long, RejectReason>,
        orderedPhotoIds: List<Long>? = null
    ) {
        // savedStateHandle에 저장하여 loadPhotos()에서 읽을 수 있도록 함
        savedStateHandleRef["initial_selection_map"] = selectionMap
        savedStateHandleRef["initial_reject_candidates"] = rejectCandidates
        if (orderedPhotoIds != null) {
            savedStateHandleRef["initial_ordered_photo_ids"] = orderedPhotoIds
        } else {
            savedStateHandleRef.remove<List<Long>>("initial_ordered_photo_ids")
        }
        
        initialSelectionMap = selectionMap
        initialRejectCandidates = rejectCandidates
        initialOrderedPhotoIds = orderedPhotoIds ?: emptyList()
        
        // 이미 로드된 경우 상태 업데이트
        if (_uiState.value is ViewerUiState.Success) {
            _uiState.update { currentState ->
                if (currentState is ViewerUiState.Success) {
                    val photosWithRejectState = currentState.photos.map { photo ->
                        // Apply reject candidates if needed
                        applyRejectCandidate(photo, rejectCandidates)
                    }
                    val updatedPhotos = applyOrderedPhotoIds(
                        photos = photosWithRejectState,
                        orderedPhotoIds = orderedPhotoIds ?: emptyList()
                    )
                    currentState.copy(
                        photos = updatedPhotos,
                        initialIndex = currentState.initialIndex,
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
                val currentOrderedPhotoIds = savedStateHandleRef.get<List<Long>>("initial_ordered_photo_ids")
                    ?: initialOrderedPhotoIds

                // 각 Photo의 recommendationScore를 DB에서 로드하고 Reject Candidate 반영
                val photosWithScores = allPhotos.map { photo ->
                    var photoWithScore = photo
                    val scoreEntity = photoScoreDao.getScore(photo.id)

                    if (scoreEntity != null) {
                        // DB에서 로드한 점수에서 rejectReason을 제거
                        // (rejectReason은 세션 기반 UI 상태로만 취급)
                        val scoreWithoutReason = scoreEntity.score.copy(rejectReason = null)
                        photoWithScore = photo.copy(recommendationScore = scoreWithoutReason)
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
                val orderedFilteredPhotos = applyOrderedPhotoIds(
                    photos = filteredPhotos,
                    orderedPhotoIds = currentOrderedPhotoIds
                )
                
                if (orderedFilteredPhotos.isEmpty()) {
                    _uiState.value = ViewerUiState.Error("사진이 없는 폴더입니다.")
                } else {
                    // 초기 인덱스 계산
                    val initialIndex = if (initialPhotoId != null) {
                        val index = orderedFilteredPhotos.indexOfFirst { it.id == initialPhotoId }
                        if (index != -1) index else 0
                    } else {
                        0
                    }
                    
                    _uiState.value = ViewerUiState.Success(
                        photos = orderedFilteredPhotos,
                        initialIndex = initialIndex,
                        selectionMap = if (selectedOnly) {
                            currentSelectionMap.filterKeys { key ->
                                orderedFilteredPhotos.any { it.id == key }
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

    private fun applyRejectCandidate(photo: Photo, candidates: Map<Long, RejectReason>): Photo {
        val candidateReason = candidates[photo.id] ?: return photo
        
        val currentScore = photo.recommendationScore ?: RecommendationScore.DEFAULT
        return photo.copy(
            recommendationScore = currentScore.copy(rejectReason = candidateReason)
        )
    }

    private fun applyOrderedPhotoIds(
        photos: List<Photo>,
        orderedPhotoIds: List<Long>
    ): List<Photo> {
        if (photos.isEmpty() || orderedPhotoIds.isEmpty()) return photos

        val photoById = photos.associateBy { it.id }
        val orderedPhotos = orderedPhotoIds.mapNotNull { id -> photoById[id] }
        if (orderedPhotos.isEmpty()) return photos

        // 전달된 순서에 없는 항목은 기존 정렬 순서를 유지한 채 뒤에 붙입니다.
        val orderedIdSet = orderedPhotos.map { it.id }.toSet()
        val remainingPhotos = photos.filterNot { orderedIdSet.contains(it.id) }
        return orderedPhotos + remainingPhotos
    }

    fun toggleSelection(photoId: Long) {
        updateSelectionState(photoId, PhotoSelectionState.Selected)
    }

    fun toggleRejection(photoId: Long) {
        updateSelectionState(photoId, PhotoSelectionState.Rejected)
    }

    private fun updateSelectionState(photoId: Long, targetState: PhotoSelectionState) {
        var shouldAdvance = false
        var isLastPage = false
        var targetIndex = -1

        _uiState.update { currentState ->
            if (currentState is ViewerUiState.Success) {
                val currentMap = currentState.selectionMap.toMutableMap()
                val currentSelection = currentMap[photoId] ?: PhotoSelectionState.None
                val newState = if (currentSelection == targetState) PhotoSelectionState.None else targetState

                shouldAdvance = newState != PhotoSelectionState.None

                val currentIndex = currentState.photos.indexOfFirst { it.id == photoId }
                isLastPage = currentIndex == currentState.photos.lastIndex
                targetIndex = currentIndex + 1

                currentMap[photoId] = newState
                currentState.copy(selectionMap = currentMap)
            } else {
                currentState
            }
        }

        viewModelScope.launch {
            if (shouldAdvance) {
                if (isLastPage) _autoAdvanceEvent.emit(AutoAdvanceEvent.AtLastPage)
                else _autoAdvanceEvent.emit(AutoAdvanceEvent.Advance(targetIndex = targetIndex))
            }
        }
    }
}
