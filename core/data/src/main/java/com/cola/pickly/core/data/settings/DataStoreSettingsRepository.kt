package com.cola.pickly.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.cola.pickly.core.model.RejectReason
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
            themeMode = prefs.getEnum(KEY_THEME_MODE, ThemeMode.System),
            smartDiscardCriteria = prefs[KEY_SMART_DISCARD_CRITERIA]?.mapNotNull { name ->
                runCatching { RejectReason.valueOf(name) }.getOrNull()
            }?.toSet() ?: RejectReason.entries.toSet(),
            smartDiscardThresholds = SmartDiscardThresholds(
                blurThreshold = prefs[KEY_BLUR_THRESHOLD] ?: 100.0f,
                minFaceSize = prefs[KEY_MIN_FACE_SIZE] ?: 0.05f,
                headAngleLimit = prefs[KEY_HEAD_ANGLE_LIMIT] ?: 30.0f,
                eyeOpenThreshold = prefs[KEY_EYE_OPEN_THRESHOLD] ?: 0.50f,
                smileExceptionThreshold = prefs[KEY_SMILE_EXCEPTION_THRESHOLD] ?: 0.70f
            ),
            debugOptions = DebugOptions(
                showFaceBoundingBox = prefs[KEY_SHOW_FACE_BOX] ?: true,
                showScoreOverlay = prefs[KEY_SHOW_SCORE] ?: false
            )
        )
    }

    override suspend fun setDuplicateFilenamePolicy(policy: DuplicateFilenamePolicy) {
        dataStore.edit { it[KEY_DUPLICATE_FILENAME] = policy.name }
    }

    override suspend fun setSmartDiscardCriteria(criteria: Set<RejectReason>) {
        dataStore.edit { it[KEY_SMART_DISCARD_CRITERIA] = criteria.map { r -> r.name }.toSet() }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setSmartDiscardThresholds(thresholds: SmartDiscardThresholds) {
        dataStore.edit { prefs ->
            prefs[KEY_BLUR_THRESHOLD] = thresholds.blurThreshold
            prefs[KEY_MIN_FACE_SIZE] = thresholds.minFaceSize
            prefs[KEY_HEAD_ANGLE_LIMIT] = thresholds.headAngleLimit
            prefs[KEY_EYE_OPEN_THRESHOLD] = thresholds.eyeOpenThreshold
            prefs[KEY_SMILE_EXCEPTION_THRESHOLD] = thresholds.smileExceptionThreshold
        }
    }

    override suspend fun setDebugOptions(options: DebugOptions) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_FACE_BOX] = options.showFaceBoundingBox
            prefs[KEY_SHOW_SCORE] = options.showScoreOverlay
        }
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
        val KEY_THEME_MODE = stringPreferencesKey("settings.theme_mode")
        val KEY_SMART_DISCARD_CRITERIA = stringSetPreferencesKey("settings.smart_discard_criteria")

        // SmartDiscardThresholds keys
        val KEY_BLUR_THRESHOLD = floatPreferencesKey("settings.blur_threshold")
        val KEY_MIN_FACE_SIZE = floatPreferencesKey("settings.min_face_size")
        val KEY_HEAD_ANGLE_LIMIT = floatPreferencesKey("settings.head_angle_limit")
        val KEY_EYE_OPEN_THRESHOLD = floatPreferencesKey("settings.eye_open_threshold")
        val KEY_SMILE_EXCEPTION_THRESHOLD = floatPreferencesKey("settings.smile_exception_threshold")

        // DebugOptions keys
        val KEY_SHOW_FACE_BOX = booleanPreferencesKey("settings.debug.show_face_box")
        val KEY_SHOW_SCORE = booleanPreferencesKey("settings.debug.show_score")
    }
}


