package com.cola.pickly.core.model

/**
 * 스마트 자동 제외 기능에서 사진이 제외된 이유를 나타내는 enum
 *
 * - label: 사용자에게 표시할 짧은 이름
 * - hasSensitivity: true이면 사용자가 조정 가능한 민감도 슬라이더를 제공합니다.
 */
enum class RejectReason(val label: String, val hasSensitivity: Boolean) {
    /**
     * 얼굴이 검출되지 않음 — 민감도 파라미터 없음
     */
    NO_FACE("인물없음", false),

    /**
     * 사진이 흔들려서 선명하지 않음
     */
    BLURRY("흔들림", true),

    /**
     * 얼굴 크기가 너무 작음
     */
    TOO_SMALL("얼굴작음", true),

    /**
     * 얼굴의 주요 부위가 가려짐 — 민감도 파라미터 없음
     */
    OCCLUDED("얼굴가림", false),

    /**
     * 눈을 감고 있음
     */
    EYES_CLOSED("눈감음", true),

    /**
     * 얼굴이 잘려서 일부만 보임 — 민감도 파라미터 없음
     */
    CROPPED("얼굴잘림", false),

    /**
     * 고개를 과도하게 돌림 (옆/위/아래를 보는 경우)
     */
    HEAD_TURNED("고개돌림", true)
}
