package com.cola.pickly.feature.organize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.domain.repository.PhotoRepository
import com.cola.pickly.core.domain.refresh.PhotoDataRefreshNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderSelectViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val photoDataRefreshNotifier: PhotoDataRefreshNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow<FolderSelectUiState>(FolderSelectUiState.Loading)
    val uiState: StateFlow<FolderSelectUiState> = _uiState.asStateFlow()

    init {
        refreshFolders()
        observeRefreshEvents()
    }

    /**
     * 폴더 목록을 다시 로드합니다.
     * @param silent true면 로딩 인디케이터를 표시하지 않고 기존 데이터를 유지한 채 갱신.
     *               false(기본값)면 Loading 상태로 전환 후 갱신 (초기 로드 시 사용).
     */
    fun refreshFolders(silent: Boolean = false) {
        viewModelScope.launch { loadFolders(showLoading = !silent) }
    }

    private fun observeRefreshEvents() {
        viewModelScope.launch {
            photoDataRefreshNotifier.refreshEvents.collectLatest {
                // 어떤 이유든 폴더 카운트/썸네일 변화 가능성이 있으므로 목록을 갱신합니다.
                loadFolders(showLoading = false)
            }
        }
    }

    private suspend fun loadFolders(showLoading: Boolean = true) {
        if (showLoading) _uiState.value = FolderSelectUiState.Loading
        try {
            val folders = photoRepository.getFolders()
            _uiState.value = FolderSelectUiState.Success(folders)
        } catch (e: Exception) {
            _uiState.value = FolderSelectUiState.Error(e.message ?: "알 수 없는 에러가 발생했습니다.")
        }
    }
}
