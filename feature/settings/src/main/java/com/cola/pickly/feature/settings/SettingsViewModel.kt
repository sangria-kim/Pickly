package com.cola.pickly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cola.pickly.core.data.cache.CacheRepository
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.SettingsRepository
import com.cola.pickly.core.data.settings.SmartDiscardResultMode
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
    private val cacheRepository: CacheRepository,
    @javax.inject.Named("isDebugBuild") private val isDebugBuild: Boolean,
    @javax.inject.Named("appVersion") private val appVersion: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            appVersion = appVersion,
            isDebugBuild = isDebugBuild
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    private var versionTapCount = 0

    init {
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        duplicateFilenamePolicy = settings.duplicateFilenamePolicy,
                        themeMode = settings.themeMode,
                        smartDiscardCriteria = settings.smartDiscardCriteria,
                        smartDiscardResultMode = settings.smartDiscardResultMode,
                        hasShownAutoRejectWarning = settings.hasShownAutoRejectWarning,
                        debugOptions = settings.debugOptions,
                        smartDiscardThresholds = settings.smartDiscardThresholds
                    )
                }
            }
        }

        refreshCacheSize()
    }

    fun setDuplicateFilenamePolicy(policy: DuplicateFilenamePolicy) {
        viewModelScope.launch { settingsRepository.setDuplicateFilenamePolicy(policy) }
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

    fun setSmartDiscardResultMode(mode: SmartDiscardResultMode) {
        viewModelScope.launch {
            // 자동 제외 모드이고 경고를 아직 보여주지 않았다면 다이얼로그 표시
            if (mode == SmartDiscardResultMode.AutoReject && !_uiState.value.hasShownAutoRejectWarning) {
                _events.emit(SettingsEvent.ShowAutoRejectWarningDialog)
            } else {
                settingsRepository.setSmartDiscardResultMode(mode)
            }
        }
    }

    fun confirmAutoRejectMode() {
        viewModelScope.launch {
            settingsRepository.setSmartDiscardResultMode(SmartDiscardResultMode.AutoReject)
            settingsRepository.setHasShownAutoRejectWarning(true)
        }
    }

    fun cancelAutoRejectMode() {
        // 모드 변경 없이 다이얼로그만 닫음
        // UI에서 라디오 버튼이 원래 선택으로 돌아가도록 처리
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

    // 디버그 메뉴 관련
    fun onVersionTap() {
        if (!isDebugBuild) return

        versionTapCount++
        if (versionTapCount >= 7) {
            _uiState.update { it.copy(isDebugMenuVisible = true) }
        }
    }

    fun setDebugOption(type: DebugOptionType, enabled: Boolean) {
        viewModelScope.launch {
            val currentOptions = _uiState.value.debugOptions
            val newOptions = when (type) {
                DebugOptionType.SHOW_DEBUG_OVERLAY -> currentOptions.copy(showDebugOverlay = enabled)
                DebugOptionType.SHOW_FACE_BOX -> currentOptions.copy(showFaceBoundingBox = enabled)
                DebugOptionType.SHOW_SCORE -> currentOptions.copy(showScoreOverlay = enabled)
            }
            settingsRepository.setDebugOptions(newOptions)
        }
    }

    fun setThreshold(type: ThresholdType, value: Float) {
        viewModelScope.launch {
            val currentThresholds = _uiState.value.smartDiscardThresholds
            val newThresholds = when (type) {
                ThresholdType.BLUR -> currentThresholds.copy(blurThreshold = value)
                ThresholdType.MIN_FACE_SIZE -> currentThresholds.copy(minFaceSize = value)
                ThresholdType.HEAD_ANGLE -> currentThresholds.copy(headAngleLimit = value)
                ThresholdType.EYE_OPEN -> currentThresholds.copy(eyeOpenThreshold = value)
                ThresholdType.SMILE_EXCEPTION -> currentThresholds.copy(smileExceptionThreshold = value)
            }
            settingsRepository.setSmartDiscardThresholds(newThresholds)
        }
    }

    fun resetSmartDiscardThresholds() {
        viewModelScope.launch {
            settingsRepository.setSmartDiscardThresholds(com.cola.pickly.core.data.settings.SmartDiscardThresholds())
        }
    }
}

sealed interface SettingsEvent {
    data object CacheCleared : SettingsEvent
    data object CacheClearFailed : SettingsEvent
    data object ShowAutoRejectWarningDialog : SettingsEvent
}

enum class DebugOptionType {
    SHOW_DEBUG_OVERLAY,
    SHOW_FACE_BOX,
    SHOW_SCORE
}

enum class ThresholdType {
    BLUR,
    MIN_FACE_SIZE,
    HEAD_ANGLE,
    EYE_OPEN,
    SMILE_EXCEPTION
}


