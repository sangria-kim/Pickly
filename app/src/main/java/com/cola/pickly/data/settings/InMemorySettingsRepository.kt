package com.cola.pickly.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI-only 단계용 In-Memory 설정 저장소.
 *
 * - 앱 프로세스가 살아있는 동안만 값이 유지됩니다.
 * - 이후 DataStore/Room 기반 구현체로 교체 대상입니다.
 */
@Singleton
class InMemorySettingsRepository @Inject constructor() : SettingsRepository {

    private val state = MutableStateFlow(Settings())

    override val settings: Flow<Settings> = state

    override suspend fun setResultSaveLocationPolicy(policy: ResultSaveLocationPolicy) {
        state.update { it.copy(resultSaveLocationPolicy = policy) }
    }

    override suspend fun setDuplicateFilenamePolicy(policy: DuplicateFilenamePolicy) {
        state.update { it.copy(duplicateFilenamePolicy = policy) }
    }

    override suspend fun setRecommendationEnabled(enabled: Boolean) {
        state.update { it.copy(isRecommendationEnabled = enabled) }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.update { it.copy(themeMode = mode) }
    }
}


