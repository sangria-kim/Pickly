package com.cola.pickly.feature.settings

import com.cola.pickly.core.data.settings.DebugOptions
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import com.cola.pickly.core.data.settings.SmartDiscardThresholds
import com.cola.pickly.core.data.settings.ThemeMode

/**
 * S-08 Settings 화면의 UI 상태.
 *
 * - Wireframe.md의 원칙처럼 설정은 '정책(Policy) 값'만 다루며, 저장 버튼 없이 즉시 반영됩니다.
 * - 실제 영속 저장은 SettingsRepository 구현체 교체로 확장합니다.
 */
data class SettingsUiState(
    val duplicateFilenamePolicy: DuplicateFilenamePolicy = DuplicateFilenamePolicy.Skip,
    val themeMode: ThemeMode = ThemeMode.System,
    val smartDiscardCriteria: Set<com.cola.pickly.core.model.RejectReason> = com.cola.pickly.core.model.RejectReason.entries.toSet(),
    val isCacheSizeLoading: Boolean = true,
    val cacheSizeBytes: Long? = null,
    val isClearingCache: Boolean = false,

    // 디버그 관련
    val appVersion: String = "1.0.0",
    val isDebugBuild: Boolean = false,
    val isDebugMenuVisible: Boolean = false,
    val debugOptions: DebugOptions = DebugOptions(),
    val smartDiscardThresholds: SmartDiscardThresholds = SmartDiscardThresholds()
)


