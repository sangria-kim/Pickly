package com.cola.pickly.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SettingsRepository의 DataStore(Preferences) 기반 구현체.
 *
 * - 앱 재실행 후에도 설정값이 유지됩니다.
 * - 저장 포맷은 enum은 name(String), boolean은 Boolean을 사용합니다.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            duplicateFilenamePolicy = prefs.getEnum(KEY_DUPLICATE_FILENAME, DuplicateFilenamePolicy.Skip),
            isRecommendationEnabled = prefs[KEY_RECOMMENDATION_ENABLED] ?: false,
            themeMode = prefs.getEnum(KEY_THEME_MODE, ThemeMode.System)
        )
    }

    override suspend fun setDuplicateFilenamePolicy(policy: DuplicateFilenamePolicy) {
        dataStore.edit { it[KEY_DUPLICATE_FILENAME] = policy.name }
    }

    override suspend fun setRecommendationEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_RECOMMENDATION_ENABLED] = enabled }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    private fun <T : Enum<T>> Preferences.getEnum(key: Preferences.Key<String>, default: T): T {
        val raw = this[key] ?: return default
        // 기존 AutoRename 데이터를 Skip으로 마이그레이션
        if (key == KEY_DUPLICATE_FILENAME && raw == "AutoRename") {
            return DuplicateFilenamePolicy.Skip as T
        }
        return runCatching { java.lang.Enum.valueOf(default.declaringJavaClass, raw) }.getOrDefault(default)
    }

    private companion object {
        val KEY_DUPLICATE_FILENAME = stringPreferencesKey("settings.duplicate_filename_policy")
        val KEY_RECOMMENDATION_ENABLED = booleanPreferencesKey("settings.recommendation_enabled")
        val KEY_THEME_MODE = stringPreferencesKey("settings.theme_mode")
    }
}


