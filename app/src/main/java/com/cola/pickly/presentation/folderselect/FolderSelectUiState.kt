package com.cola.pickly.presentation.folderselect

import com.cola.pickly.domain.model.PhotoFolder

sealed interface FolderSelectUiState {
    data object Loading : FolderSelectUiState
    data class Success(val folders: List<PhotoFolder>) : FolderSelectUiState
    data class Error(val message: String) : FolderSelectUiState
}
