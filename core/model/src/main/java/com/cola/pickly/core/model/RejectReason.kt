package com.cola.pickly.core.model

/**
 * 스마트 자동 제외 기능에서 사진이 제외된 이유를 나타내는 enum
 *
 * 각 사유는 사용자에게 표시할 label을 포함합니다.
 */
enum class RejectReason(val label: String) {
    /**
     * 얼굴이 검출되지 않음
     */
    NO_FACE("인물없음"),

    /**
     * 사진이 흔들려서 선명하지 않음
     */
    BLURRY("흔들림"),

    /**
     * 얼굴 크기가 너무 작음
     */
    TOO_SMALL("얼굴작음"),

    /**
     * 얼굴의 주요 부위가 가려짐
     */
    OCCLUDED("얼굴가림"),

    /**
     * 눈을 감고 있음
     */
    EYES_CLOSED("눈감음"),

    /**
     * 얼굴이 잘려서 일부만 보임
     */
    CROPPED("얼굴잘림"),

    /**
     * 고개를 과도하게 돌림 (옆/위/아래를 보는 경우)
     */
    HEAD_TURNED("고개돌림")
}
