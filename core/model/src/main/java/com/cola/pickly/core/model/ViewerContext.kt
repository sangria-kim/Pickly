package com.cola.pickly.core.model

/**
 * 풀스크린 뷰어(S-04)의 진입 컨텍스트를 정의합니다.
 * 
 * 동일한 ViewerScreen을 사용하지만, 컨텍스트에 따라 UI와 기능이 다르게 동작합니다.
 */
enum class ViewerContext {
    /**
     * 정리하기 탭(Tab 1)에서 진입
     * - 채택/제외 버튼 표시
     * - 상태 변경 가능
     */
    SELECT,
    
    /**
     * 모아보기 탭(Tab 2)에서 진입
     * - 읽기 전용 (하단 UI 미표시)
     * - 상태 변경 불가
     * - 감상 중심 UX
     */
    ARCHIVE
}
