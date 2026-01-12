package com.cola.pickly.feature.weekly

import com.cola.pickly.core.model.WeekId

/** 주차 리스트 화면의 초기 표시 개수 */
const val INITIAL_VISIBLE_WEEKS = 4

/**
 * 주차별 리스트 화면의 전체 UI 상태를 나타냅니다.
 * @property isLoading 데이터를 로딩 중인지 여부.
 * @property weeklyList 화면에 표시될 모든 주차 정보 리스트.
 * @property errorMessage 데이터 로딩 중 에러가 발생했을 경우의 메시지.
 * @property visibleWeekCount 현재 화면에 보여주고 있는 주차 카드의 개수.
 */
data class WeeklyListUiState(
    val isLoading: Boolean = false,
    val weeklyList: List<WeekUiModel> = emptyList(),
    val errorMessage: String? = null,
    val visibleWeekCount: Int = INITIAL_VISIBLE_WEEKS
) {
    /**
     * 데이터 로딩에 성공했는지 여부를 나타냅니다.
     * (로딩 중이 아니며, 에러가 없는 상태)
     */
    val isSuccess: Boolean
        get() = !isLoading && errorMessage == null

    /**
     * 데이터 로딩에 성공했지만, 표시할 콘텐츠가 없는 상태를 나타냅니다.
     */
    val isEmpty: Boolean
        get() = isSuccess && weeklyList.isEmpty()

    /**
     * '더보기' 버튼을 통해 추가로 로드할 주차가 남아있는지 여부를 나타냅니다.
     */
    val canLoadMore: Boolean
        get() = weeklyList.size > visibleWeekCount
}

/**
 * 리스트 화면의 각 주차 카드 하나를 표현하는 UI 모델입니다.
 * @property weekId 해당 주차의 고유 ID.
 * @property title 화면에 표시될 제목 (예: "2025년 30주차").
 * @property periodText 화면에 표시될 기간 (예: "2025-07-21 ~ 2025-07-27").
 * @property bestPhotos 해당 주차의 추천 사진 파일 경로 리스트.
 */
data class WeekUiModel(
    val weekId: WeekId,
    val title: String,
    val periodText: String, 
    val bestPhotos: List<String> 
)
