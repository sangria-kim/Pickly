package com.cola.pickly.presentation.viewer

import com.cola.pickly.domain.model.WeeklyPhoto

sealed interface ViewerUiState {
    data object Loading : ViewerUiState
    
    data class Success(
        val photos: List<WeeklyPhoto>,
        val initialIndex: Int = 0,
        val selectionMap: Map<Long, PhotoSelectionState> = emptyMap()
    ) : ViewerUiState
    
    data class Error(val message: String) : ViewerUiState
}

enum class PhotoSelectionState {
    None, Selected, Rejected
}
