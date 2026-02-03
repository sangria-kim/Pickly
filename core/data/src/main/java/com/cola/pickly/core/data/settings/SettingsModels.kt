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
 * 스마트 사진 제외 기능의 임계값 설정.
 *
 * PhotoQualityAnalyzer가 사진을 분석할 때 사용하는 기준값들입니다.
 */
data class SmartDiscardThresholds(
    val blurThreshold: Double = 100.0,           // 흔들림 임계값 (Laplacian variance)
    val minFaceSize: Float = 0.05f,              // 얼굴 최소 크기 (이미지 너비 대비 비율)
    val eyeOpenThreshold: Float = 0.5f,          // 눈 뜸 임계값
    val smileExceptionThreshold: Float = 0.7f,   // 눈웃음 예외 임계값
    val headAngleLimit: Float = 30.0f            // 고개 각도 제한 (도)
)

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
    val isSmartDiscardReasonEnabled: Boolean = true,
    val smartDiscardCriteria: Set<com.cola.pickly.core.model.RejectReason> = com.cola.pickly.core.model.RejectReason.entries.toSet(),
    val smartDiscardThresholds: SmartDiscardThresholds = SmartDiscardThresholds()
)


