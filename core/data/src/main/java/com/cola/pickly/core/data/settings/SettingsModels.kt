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
    val isRecommendationEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System
)


