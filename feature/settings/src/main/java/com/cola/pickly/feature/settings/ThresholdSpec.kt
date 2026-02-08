package com.cola.pickly.feature.settings

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
            ThresholdType.HEAD_ANGLE -> ThresholdSpec(range = 10f..60f, step = 0.5f, decimals = 1)
            ThresholdType.EYE_OPEN -> ThresholdSpec(range = 0.10f..0.90f, step = 0.01f, decimals = 2)
            ThresholdType.SMILE_EXCEPTION -> ThresholdSpec(range = 0.00f..1.00f, step = 0.05f, decimals = 2)
        }
}

private fun Float.roundToDecimals(decimals: Int): Float {
    if (decimals <= 0) return this.roundToInt().toFloat()
    val factor = 10f.pow(decimals)
    return (this * factor).roundToInt() / factor
}
