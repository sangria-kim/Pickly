package com.cola.pickly.core.data.settings

/**
 * S-08 설정 화면에서 사용하는 정책(Policy) 값들.
 *
 * - UI-only 단계에서도 ViewModel/Screen이 타입 안정적으로 상태를 다룰 수 있도록 모델을 먼저 정의합니다.
 * - 영속 저장(DataStore/Room 등)은 이후 SettingsRepository 구현체 교체로 확장합니다.
 */

/**
 * A-02. 동일 파일명 처리 방식
 */
enum class DuplicateFilenamePolicy {
    Overwrite,
    Skip
}

/**
 * C-01. 테마
 */
enum class ThemeMode {
    System,
    Light,
    Dark
}

/**
 * 설정 값 묶음(단일 소스).
 *
 * Wireframe.md의 정책 기본값을 우선 반영:
 * - 사진 추천 기능: OFF
 * - 테마: 시스템 설정 따름
 *
 * 나머지 기본값은 명세에 명확한 디폴트가 없어, 안전한(데이터 손실 방지) 쪽으로 설정합니다.
 */
data class Settings(
    val duplicateFilenamePolicy: DuplicateFilenamePolicy = DuplicateFilenamePolicy.Skip,
    val themeMode: ThemeMode = ThemeMode.System,
    val smartDiscardCriteria: Set<com.cola.pickly.core.model.RejectReason> = com.cola.pickly.core.model.RejectReason.entries.toSet(),
    val smartDiscardThresholds: SmartDiscardThresholds = SmartDiscardThresholds(),
    val debugOptions: DebugOptions = DebugOptions()
)

/**
 * 스마트 사진 자동 제외 기능의 분석 파라미터 임계값.
 *
 * 디버그 메뉴에서 튜닝 가능하며, 각 임계값은 사진 분석 시 사용됩니다.
 */
data class SmartDiscardThresholds(
    val blurThreshold: Float = 100.0f,
    val minFaceSize: Float = 0.05f,
    val headAngleLimit: Float = 30.0f,
    val eyeOpenThreshold: Float = 0.50f,
    val smileExceptionThreshold: Float = 0.70f
)

/**
 * 디버그 전용 옵션 (개발자 메뉴).
 *
 * ViewerScreen에서 분석 결과를 시각화하는 옵션들입니다.
 * Release 빌드에서는 디버그 메뉴 자체가 노출되지 않습니다.
 */
data class DebugOptions(
    val showFaceBoundingBox: Boolean = true,
    val showRejectReasonOverlay: Boolean = true,
    val showScoreOverlay: Boolean = false
)


