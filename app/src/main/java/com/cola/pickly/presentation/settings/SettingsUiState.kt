package com.cola.pickly.presentation.settings

import com.cola.pickly.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.data.settings.ResultSaveLocationPolicy
import com.cola.pickly.data.settings.ThemeMode

/**
 * S-08 Settings 화면의 UI 상태.
 *
 * - Wireframe.md의 원칙처럼 설정은 '정책(Policy) 값'만 다루며, 저장 버튼 없이 즉시 반영됩니다.
 * - 실제 영속 저장은 SettingsRepository 구현체 교체로 확장합니다.
 */
data class SettingsUiState(
    val resultSaveLocationPolicy: ResultSaveLocationPolicy = ResultSaveLocationPolicy.RememberLastUsedFolder,
    val duplicateFilenamePolicy: DuplicateFilenamePolicy = DuplicateFilenamePolicy.AutoRename,
    val isRecommendationEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val isCacheSizeLoading: Boolean = true,
    val cacheSizeBytes: Long? = null,
    val isClearingCache: Boolean = false
)


