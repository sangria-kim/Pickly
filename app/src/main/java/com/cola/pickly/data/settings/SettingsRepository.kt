package com.cola.pickly.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * 설정 데이터 소스 계약.
 *
 * - UI-only 단계에서는 InMemory 구현체로도 화면/상태 흐름을 만들 수 있습니다.
 * - 추후 DataStore/Room 기반 구현체로 교체해도 Presentation 계층 변경이 최소화되도록 계약을 고정합니다.
 */
interface SettingsRepository {
    val settings: Flow<Settings>

    suspend fun setResultSaveLocationPolicy(policy: ResultSaveLocationPolicy)

    suspend fun setDuplicateFilenamePolicy(policy: DuplicateFilenamePolicy)

    suspend fun setRecommendationEnabled(enabled: Boolean)

    suspend fun setThemeMode(mode: ThemeMode)
}


