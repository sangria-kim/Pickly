package com.cola.pickly.feature.settings

import com.cola.pickly.core.data.settings.SmartDiscardSensitivities
import com.cola.pickly.core.data.settings.SmartDiscardThresholds
import com.cola.pickly.core.model.SensitivityLevel
import kotlin.math.pow
import kotlin.math.roundToInt

data class ThresholdSpec(
    val range: ClosedFloatingPointRange<Float>,
    val step: Float,
    val decimals: Int
) {
    val steps: Int
        get() {
            val span = range.endInclusive - range.start
            val tickCount = (span / step).roundToInt()
            // Slider steps = tickCount - 1 (between min..max inclusive)
            return (tickCount - 1).coerceAtLeast(0)
        }

    fun normalize(raw: Float): Float {
        val clamped = raw.coerceIn(range.start, range.endInclusive)
        val tick = ((clamped - range.start) / step).roundToInt()
        val snapped = range.start + (tick * step)
        return snapped
            .roundToDecimals(decimals)
            .coerceIn(range.start, range.endInclusive)
    }
}

object ThresholdSpecs {
    fun forType(type: ThresholdType): ThresholdSpec =
        when (type) {
            ThresholdType.BLUR -> ThresholdSpec(range = 30f..250f, step = 1f, decimals = 0)
            ThresholdType.MIN_FACE_SIZE -> ThresholdSpec(range = 0.02f..0.20f, step = 0.005f, decimals = 3)
            ThresholdType.EYE_OPEN -> ThresholdSpec(range = 0.10f..0.90f, step = 0.01f, decimals = 2)
            ThresholdType.SMILE_EXCEPTION -> ThresholdSpec(range = 0.00f..1.00f, step = 0.05f, decimals = 2)
        }
}

/**
 * SensitivityLevel ↔ SmartDiscardThresholds 변환 매핑.
 *
 * | Level | 레이블       | BLUR↓ | EYE_OPEN↑ | MIN_FACE↑ |
 * |-------|------------|--------|-----------|-----------|
 * | 1     | 많이 낮음  | 120    | 0.05      | 0.02      |
 * | 2     | 낮음       | 100    | 0.15      | 0.03      |
 * | 3     | 약간 낮음  | 80     | 0.25      | 0.04      |
 * | 4     | 보통(권장) | 55     | 0.35      | 0.05      |
 * | 5     | 약간 높음  | 40     | 0.45      | 0.08      |
 * | 6     | 높음       | 30     | 0.55      | 0.11      |
 * | 7     | 매우 높음  | 20     | 0.70      | 0.14      |
 *
 * smileExceptionThreshold는 eyeOpen Level과 연동해 자동 계산됩니다.
 * (Level이 높아질수록 예외를 더 좁게 적용 → 값이 낮아짐)
 */
object SensitivityPresets {
    fun blurFor(level: SensitivityLevel): Float = when (level) {
        SensitivityLevel.LEVEL_1 -> 120f
        SensitivityLevel.LEVEL_2 -> 100f
        SensitivityLevel.LEVEL_3 -> 80f
        SensitivityLevel.LEVEL_4 -> 55f
        SensitivityLevel.LEVEL_5 -> 40f
        SensitivityLevel.LEVEL_6 -> 30f
        SensitivityLevel.LEVEL_7 -> 20f
    }

    fun eyeOpenFor(level: SensitivityLevel): Float = when (level) {
        SensitivityLevel.LEVEL_1 -> 0.05f
        SensitivityLevel.LEVEL_2 -> 0.15f
        SensitivityLevel.LEVEL_3 -> 0.25f
        SensitivityLevel.LEVEL_4 -> 0.35f
        SensitivityLevel.LEVEL_5 -> 0.45f
        SensitivityLevel.LEVEL_6 -> 0.55f
        SensitivityLevel.LEVEL_7 -> 0.70f
    }

    fun minFaceSizeFor(level: SensitivityLevel): Float = when (level) {
        SensitivityLevel.LEVEL_1 -> 0.02f
        SensitivityLevel.LEVEL_2 -> 0.03f
        SensitivityLevel.LEVEL_3 -> 0.04f
        SensitivityLevel.LEVEL_4 -> 0.05f
        SensitivityLevel.LEVEL_5 -> 0.08f
        SensitivityLevel.LEVEL_6 -> 0.11f
        SensitivityLevel.LEVEL_7 -> 0.14f
    }

    fun smileExceptionFor(level: SensitivityLevel): Float = when (level) {
        SensitivityLevel.LEVEL_1 -> 0.80f
        SensitivityLevel.LEVEL_2 -> 0.75f
        SensitivityLevel.LEVEL_3 -> 0.70f
        SensitivityLevel.LEVEL_4 -> 0.60f
        SensitivityLevel.LEVEL_5 -> 0.55f
        SensitivityLevel.LEVEL_6 -> 0.45f
        SensitivityLevel.LEVEL_7 -> 0.35f
    }

    /** SmartDiscardSensitivities → SmartDiscardThresholds 통합 변환 (디버그 동기화용) */
    fun toThresholds(s: SmartDiscardSensitivities): SmartDiscardThresholds = SmartDiscardThresholds(
        blurThreshold = blurFor(s.blur),
        minFaceSize = minFaceSizeFor(s.minFaceSize),
        eyeOpenThreshold = eyeOpenFor(s.eyeOpen),
        smileExceptionThreshold = smileExceptionFor(s.eyeOpen),
    )
}

private fun Float.roundToDecimals(decimals: Int): Float {
    if (decimals <= 0) return this.roundToInt().toFloat()
    val factor = 10f.pow(decimals)
    return (this * factor).roundToInt() / factor
}
