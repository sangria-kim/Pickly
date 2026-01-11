package com.cola.pickly.presentation.folderselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderSelectViewModel @Inject constructor(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FolderSelectUiState>(FolderSelectUiState.Loading)
    val uiState: StateFlow<FolderSelectUiState> = _uiState.asStateFlow()

    init {
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            _uiState.value = FolderSelectUiState.Loading
            try {
                val folders = photoRepository.getFolders()
                _uiState.value = FolderSelectUiState.Success(folders)
            } catch (e: Exception) {
                _uiState.value = FolderSelectUiState.Error(e.message ?: "알 수 없는 에러가 발생했습니다.")
            }
        }
    }
}
