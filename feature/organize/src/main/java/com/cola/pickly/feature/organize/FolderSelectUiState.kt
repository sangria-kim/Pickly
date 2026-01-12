package com.cola.pickly.feature.organize

import com.cola.pickly.core.model.PhotoFolder

sealed interface FolderSelectUiState {
    data object Loading : FolderSelectUiState
    data class Success(val folders: List<PhotoFolder>) : FolderSelectUiState
    data class Error(val message: String) : FolderSelectUiState
}
