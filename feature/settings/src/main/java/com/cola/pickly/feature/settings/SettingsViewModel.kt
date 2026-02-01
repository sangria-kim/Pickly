package com.cola.pickly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.data.cache.CacheRepository
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.SettingsRepository
import com.cola.pickly.core.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * S-08 Settings 화면의 상태/정책 변경을 관리하는 ViewModel.
 *
 * DataStoreSettingsRepository를 통해 설정값을 영속 저장하고,
 * 변경 사항은 즉시 반영됩니다.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val cacheRepository: CacheRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        duplicateFilenamePolicy = settings.duplicateFilenamePolicy,
                        themeMode = settings.themeMode,
                        isSmartDiscardReasonEnabled = settings.isSmartDiscardReasonEnabled,
                        smartDiscardCriteria = settings.smartDiscardCriteria
                    )
                }
            }
        }

        refreshCacheSize()
    }

    fun setDuplicateFilenamePolicy(policy: DuplicateFilenamePolicy) {
        viewModelScope.launch { settingsRepository.setDuplicateFilenamePolicy(policy) }
    }

    fun setSmartDiscardReasonEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSmartDiscardReasonEnabled(enabled) }
    }

    fun toggleSmartDiscardCriterion(criterion: com.cola.pickly.core.model.RejectReason) {
        viewModelScope.launch {
            val currentCriteria = _uiState.value.smartDiscardCriteria.toMutableSet()
            if (currentCriteria.contains(criterion)) {
                currentCriteria.remove(criterion)
            } else {
                currentCriteria.add(criterion)
            }
            settingsRepository.setSmartDiscardCriteria(currentCriteria)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCacheSizeLoading = true) }
            runCatching { cacheRepository.getCacheSizeBytes() }
                .onSuccess { size ->
                    _uiState.update { it.copy(isCacheSizeLoading = false, cacheSizeBytes = size) }
                }
                .onFailure {
                    _uiState.update { it.copy(isCacheSizeLoading = false) }
                }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val wasClearing = _uiState.value.isClearingCache
            if (wasClearing) return@launch

            _uiState.update { it.copy(isClearingCache = true) }
            val result = runCatching { cacheRepository.clearCache() }
            _uiState.update { it.copy(isClearingCache = false) }

            if (result.isSuccess) {
                _events.emit(SettingsEvent.CacheCleared)
            } else {
                _events.emit(SettingsEvent.CacheClearFailed)
            }

            refreshCacheSize()
        }
    }
}

sealed interface SettingsEvent {
    data object CacheCleared : SettingsEvent
    data object CacheClearFailed : SettingsEvent
}


