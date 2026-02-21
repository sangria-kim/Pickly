package com.cola.pickly.core.model

/**
 * 스마트 제외 기준별 민감도를 나타내는 7단계 이산 enum.
 *
 * 사용자에게는 기술적 float 값 대신 단계 레이블로 노출됩니다.
 * LEVEL_4(보통)가 앱 기본값입니다.
 */
enum class SensitivityLevel(val step: Int, val label: String) {
    LEVEL_1(1, "많이 낮음"),
    LEVEL_2(2, "낮음"),
    LEVEL_3(3, "약간 낮음"),
    LEVEL_4(4, "보통(권장)"),
    LEVEL_5(5, "약간 높음"),
    LEVEL_6(6, "높음"),
    LEVEL_7(7, "매우 높음");

    companion object {
        val default = LEVEL_4

        fun fromStep(step: Int): SensitivityLevel =
            entries.find { it.step == step } ?: default
    }
}
