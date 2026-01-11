package com.cola.pickly.presentation.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 앱 전역 디버깅 옵션을 관리하는 싱글톤 객체입니다.
 */
object AppDebugConfig {
    // 점수 정보 표시 여부 (기본값 true로 설정하여 개발 중 확인 용이)
    var isShowScoreEnabled by mutableStateOf(true)

    // 얼굴 박스 그리기 여부 (기본값 true)
    var isShowFaceBoundingBoxEnabled by mutableStateOf(true)
}