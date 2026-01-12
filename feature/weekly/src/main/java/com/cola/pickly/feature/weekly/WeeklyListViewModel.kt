package com.cola.pickly.feature.weekly

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.model.WeekId
import com.cola.pickly.core.model.WeeklyGroup
import com.cola.pickly.core.model.WeeklyPhoto
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.domain.usecase.GroupPhotosByWeekUseCase
import com.cola.pickly.core.domain.usecase.PickBestPhotosUseCase
import com.cola.pickly.feature.weekly.FullScreenState
import com.cola.pickly.feature.weekly.WeeklyDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

const val WEEKS_TO_LOAD_AT_ONCE = 4

/**
 * 주간 베스트 사진 리스트 및 상세 화면의 UI 상태와 비즈니스 로직을 관리합니다.
 * 두 화면이 이 ViewModel을 공유합니다.
 */
@HiltViewModel
class WeeklyListViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val groupPhotosByWeekUseCase: GroupPhotosByWeekUseCase,
    private val pickBestPhotosUseCase: PickBestPhotosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyListUiState())
    val uiState: StateFlow<WeeklyListUiState> = _uiState

    private val _detailState = MutableStateFlow(WeeklyDetailUiState())
    val detailState: StateFlow<WeeklyDetailUiState> = _detailState

    private var originalWeeklyGroups: List<WeeklyGroup> = emptyList()

    /**
     * 저장소에서 사진을 로드하고, 주별로 그룹핑 및 추천 사진을 선택하여 UI 상태를 초기화합니다.
     * 앱 시작 시 또는 사진 접근 권한 획득 시 호출됩니다.
     */
    fun loadWeeklyBestPhotos() {
        viewModelScope.launch {
            try {
                val currentVisibleCount = _uiState.value.visibleWeekCount
                _uiState.value = WeeklyListUiState(isLoading = true, visibleWeekCount = currentVisibleCount)

                val photos = photoRepository.getRecentPhotos()
                val grouped = groupPhotosByWeekUseCase(photos)
                
                // V2: 점수 기반 추천 로직 실행 (suspend 함수)
                val recommendedGroups = pickBestPhotosUseCase(grouped)

                originalWeeklyGroups = recommendedGroups

                val uiModels = recommendedGroups.map { group ->
                    val periodText =
                        "${group.startDate.format(dateFormatter)} ~ ${group.endDate.format(dateFormatter)}"

                    WeekUiModel(
                        weekId = group.weekId,
                        title = "${group.weekId.year}년 ${group.weekId.weekOfYear}주차",
                        periodText = periodText,
                        bestPhotos = group.recommended.map { it.filePath }
                    )
                }

                _uiState.value = WeeklyListUiState(
                    isLoading = false,
                    weeklyList = uiModels,
                    visibleWeekCount = minOf(uiModels.size, currentVisibleCount)
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "알 수 없는 오류 발생"
                )
            }
        }
    }

    /**
     * 사용자가 리스트에서 특정 주차 카드를 클릭했을 때 호출됩니다.
     * 캐시된 데이터에서 해당 주차의 상세 정보를 찾아 상세 화면의 UI 상태([detailState])를 업데이트합니다.
     * @param weekId 선택된 주차의 고유 ID
     */
    fun onWeekSelected(weekId: WeekId) {
        Log.d("WeeklyListViewModel", "Week selected: $weekId")
        originalWeeklyGroups.find { it.weekId == weekId }?.let { group ->
            Log.d("WeeklyListViewModel", "Found group for detail view: $group")
            // Reset fullScreenState when a new week is selected
            _detailState.value = WeeklyDetailUiState(
                weekTitle = "${group.weekId.year}년 ${group.weekId.weekOfYear}주차",
                weekPeriod = "${group.startDate.format(dateFormatter)} ~ ${group.endDate.format(dateFormatter)}",
                recommendedPhotos = group.recommended,
                allPhotos = group.photos
            )
        }
    }

    /**
     * 상세 화면의 '전체 사진 보기' 그리드 확장/축소 상태를 토글합니다.
     */
    fun toggleAllPhotosGrid() {
        _detailState.update { it.copy(isAllPhotosExpanded = !it.isAllPhotosExpanded) }
    }

    /**
     * 주차 리스트 화면에서 '이전 주 더 보기' 버튼을 클릭했을 때 호출됩니다.
     * 표시할 주차 카드 개수를 늘려 리스트를 확장합니다.
     */
    fun loadMoreWeeks() {
        _uiState.update {
            val newCount = it.visibleWeekCount + WEEKS_TO_LOAD_AT_ONCE
            it.copy(visibleWeekCount = minOf(newCount, it.weeklyList.size))
        }
    }

    /**
     * 상세 화면에서 사진 썸네일을 탭했을 때 호출됩니다.
     * 전체 화면 뷰어에 필요한 상태를 설정합니다.
     * @param tappedPhoto 사용자가 탭한 사진 객체
     */
    fun onPhotoTapped(tappedPhoto: WeeklyPhoto) {
        _detailState.update {
            // 로드맵 규칙: [추천 사진, 나머지 사진] 순서로 중복 없는 리스트 생성
            val viewerPhotos = (it.recommendedPhotos + (it.allPhotos - it.recommendedPhotos.toSet()))
                .distinctBy { p -> p.id }

            val initialIndex = viewerPhotos.indexOf(tappedPhoto).coerceAtLeast(0)

            Log.d("WeeklyListViewModel", "Photo tapped: $tappedPhoto, Index: $initialIndex")
            Log.d("WeeklyListViewModel", "Viewer photo list size: ${viewerPhotos.size}")

            it.copy(
                fullScreenState = FullScreenState(
                    photos = viewerPhotos,
                    initialIndex = initialIndex
                )
            )
        }
    }

    /**
     * 전체 화면 뷰어가 닫혔을 때 호출됩니다.
     * 뷰어 상태를 null로 만들어 뷰어를 숨깁니다.
     */
    fun onFullScreenDismissed() {
        _detailState.update {
            it.copy(fullScreenState = null)
        }
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}