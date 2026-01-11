package com.cola.pickly.presentation.weeklydetail

import com.cola.pickly.domain.model.WeeklyPhoto

/**
 * 주 상세 화면의 UI 상태를 나타내는 데이터 클래스입니다.
 *
 * @property isLoading 데이터를 로딩하는 중인지 여부.
 * @property errorMessage 오류 발생 시 메시지.
 * @property weekTitle 주차 제목 (예: "2025년 30주차").
 * @property weekPeriod 주차 기간 (예: "2025-12-07 ~ 2025-12-13").
 * @property recommendedPhotos 추천된 사진 2장의 정보.
 * @property allPhotos 해당 주의 모든 사진 정보.
 * @property isAllPhotosExpanded 전체 사진 그리드가 펼쳐져 있는지 여부.
 * @property fullScreenState 전체 화면 뷰어의 상태. null이 아니면 뷰어를 표시합니다.
 */
data class WeeklyDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val weekTitle: String = "",
    val weekPeriod: String = "",
    val recommendedPhotos: List<WeeklyPhoto> = emptyList(),
    val allPhotos: List<WeeklyPhoto> = emptyList(),
    val isAllPhotosExpanded: Boolean = false,
    val fullScreenState: FullScreenState? = null
)

/**
 * 전체 화면 사진 뷰어의 상태를 정의합니다.
 * @property photos 뷰어에서 보여줄 사진의 전체 목록 (추천 사진 + 나머지).
 * @property initialIndex 사용자가 탭하여 뷰어를 시작할 사진의 인덱스.
 */
data class FullScreenState(
    val photos: List<WeeklyPhoto>,
    val initialIndex: Int
)
