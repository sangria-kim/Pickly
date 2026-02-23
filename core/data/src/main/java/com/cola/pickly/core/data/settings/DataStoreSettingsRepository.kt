package com.cola.pickly.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.cola.pickly.core.model.RejectReason
import com.cola.pickly.core.model.SensitivityLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

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
                blurThreshold = prefs[KEY_BLUR_THRESHOLD] ?: 55.0f,
                minFaceSize = prefs[KEY_MIN_FACE_SIZE] ?: 0.05f,
                headAngleLimit = prefs[KEY_HEAD_ANGLE_LIMIT] ?: 30.0f,
                eyeOpenThreshold = prefs[KEY_EYE_OPEN_THRESHOLD] ?: 0.50f,
                smileExceptionThreshold = prefs[KEY_SMILE_EXCEPTION_THRESHOLD] ?: 0.70f
            ),
            smartDiscardResultMode = prefs.getEnum(KEY_SMART_DISCARD_RESULT_MODE, SmartDiscardResultMode.ShowAsCandidates),
            hasShownAutoRejectWarning = prefs[KEY_HAS_SHOWN_AUTO_REJECT_WARNING] ?: false,
            debugOptions = DebugOptions(
                showDebugOverlay = prefs[KEY_SHOW_DEBUG_OVERLAY] ?: false,
                showFaceBoundingBox = prefs[KEY_SHOW_FACE_BOX] ?: true,
                showScoreOverlay = prefs[KEY_SHOW_SCORE] ?: false
            ),
            hasSeenWelcomeSplash = prefs[KEY_HAS_SEEN_WELCOME_SPLASH] ?: false,
            smartDiscardSensitivities = SmartDiscardSensitivities(
                blur = prefs[KEY_SENSITIVITY_BLUR]
                    ?.let { SensitivityLevel.fromStep(it) }
                    ?: migrateSensitivityFromFloat(
                        prefs[KEY_BLUR_THRESHOLD],
                        BLUR_FLOAT_PRESETS
                    ),
                eyeOpen = prefs[KEY_SENSITIVITY_EYE_OPEN]
                    ?.let { SensitivityLevel.fromStep(it) }
                    ?: migrateSensitivityFromFloat(
                        prefs[KEY_EYE_OPEN_THRESHOLD],
                        EYE_OPEN_FLOAT_PRESETS
                    ),
                headAngle = prefs[KEY_SENSITIVITY_HEAD_ANGLE]
                    ?.let { SensitivityLevel.fromStep(it) }
                    ?: migrateSensitivityFromFloat(
                        prefs[KEY_HEAD_ANGLE_LIMIT],
                        HEAD_ANGLE_FLOAT_PRESETS
                    ),
                minFaceSize = prefs[KEY_SENSITIVITY_MIN_FACE_SIZE]
                    ?.let { SensitivityLevel.fromStep(it) }
                    ?: migrateSensitivityFromFloat(
                        prefs[KEY_MIN_FACE_SIZE],
                        MIN_FACE_FLOAT_PRESETS
                    ),
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

    override suspend fun setSmartDiscardResultMode(mode: SmartDiscardResultMode) {
        dataStore.edit { it[KEY_SMART_DISCARD_RESULT_MODE] = mode.name }
    }

    override suspend fun setHasShownAutoRejectWarning(shown: Boolean) {
        dataStore.edit { it[KEY_HAS_SHOWN_AUTO_REJECT_WARNING] = shown }
    }

    override suspend fun setDebugOptions(options: DebugOptions) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_DEBUG_OVERLAY] = options.showDebugOverlay
            prefs[KEY_SHOW_FACE_BOX] = options.showFaceBoundingBox
            prefs[KEY_SHOW_SCORE] = options.showScoreOverlay
        }
    }

    override suspend fun setHasSeenWelcomeSplash(seen: Boolean) {
        dataStore.edit { it[KEY_HAS_SEEN_WELCOME_SPLASH] = seen }
    }

    override suspend fun setSensitivities(sensitivities: SmartDiscardSensitivities) {
        dataStore.edit { prefs ->
            prefs[KEY_SENSITIVITY_BLUR] = sensitivities.blur.step
            prefs[KEY_SENSITIVITY_EYE_OPEN] = sensitivities.eyeOpen.step
            prefs[KEY_SENSITIVITY_HEAD_ANGLE] = sensitivities.headAngle.step
            prefs[KEY_SENSITIVITY_MIN_FACE_SIZE] = sensitivities.minFaceSize.step
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

        // SmartDiscardResultMode keys
        val KEY_SMART_DISCARD_RESULT_MODE = stringPreferencesKey("settings.smart_discard_result_mode")
        val KEY_HAS_SHOWN_AUTO_REJECT_WARNING = booleanPreferencesKey("settings.has_shown_auto_reject_warning")

        // DebugOptions keys
        val KEY_SHOW_DEBUG_OVERLAY = booleanPreferencesKey("settings.debug.show_debug_overlay")
        val KEY_SHOW_FACE_BOX = booleanPreferencesKey("settings.debug.show_face_box")
        val KEY_SHOW_SCORE = booleanPreferencesKey("settings.debug.show_score")

        // Onboarding keys
        val KEY_HAS_SEEN_WELCOME_SPLASH = booleanPreferencesKey("settings.has_seen_welcome_splash")

        // SmartDiscardSensitivities keys (Int: SensitivityLevel.step)
        val KEY_SENSITIVITY_BLUR = intPreferencesKey("settings.sensitivity.blur")
        val KEY_SENSITIVITY_EYE_OPEN = intPreferencesKey("settings.sensitivity.eye_open")
        val KEY_SENSITIVITY_HEAD_ANGLE = intPreferencesKey("settings.sensitivity.head_angle")
        val KEY_SENSITIVITY_MIN_FACE_SIZE = intPreferencesKey("settings.sensitivity.min_face_size")

        // 마이그레이션용: 기존 float 값 → 가장 가까운 SensitivityLevel 매핑 테이블
        // 순서: LEVEL_1 ~ LEVEL_7
        val BLUR_FLOAT_PRESETS = listOf(120f, 100f, 80f, 55f, 40f, 30f, 20f)
        val EYE_OPEN_FLOAT_PRESETS = listOf(0.10f, 0.25f, 0.38f, 0.50f, 0.63f, 0.75f, 0.85f)
        val HEAD_ANGLE_FLOAT_PRESETS = listOf(55f, 45f, 37f, 30f, 22f, 16f, 11f)
        val MIN_FACE_FLOAT_PRESETS = listOf(0.02f, 0.03f, 0.04f, 0.05f, 0.08f, 0.11f, 0.14f)
    }

    /**
     * 기존 float 값을 가장 가까운 SensitivityLevel로 변환합니다 (1회성 마이그레이션).
     * 신규 Int 키가 없을 때만 호출됩니다.
     */
    private fun migrateSensitivityFromFloat(
        floatValue: Float?,
        presets: List<Float>
    ): SensitivityLevel {
        if (floatValue == null) return SensitivityLevel.default
        val nearestIndex = presets.indices.minByOrNull { abs(presets[it] - floatValue) }
            ?: return SensitivityLevel.default
        return SensitivityLevel.fromStep(nearestIndex + 1)
    }
}


