package com.cola.pickly.presentation.viewer

import com.cola.pickly.core.model.PhotoSelectionState
import com.cola.pickly.core.model.Photo

sealed interface ViewerUiState {
    data object Loading : ViewerUiState
    
    data class Success(
        val photos: List<Photo> = emptyList(),
        val initialIndex: Int = 0,
        val selectionMap: Map<Long, PhotoSelectionState> = emptyMap()
    ) : ViewerUiState
    
    data class Error(val message: String) : ViewerUiState
}
