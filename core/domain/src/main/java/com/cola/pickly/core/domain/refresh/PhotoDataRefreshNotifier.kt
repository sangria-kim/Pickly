package com.cola.pickly.core.domain.refresh

import kotlinx.coroutines.flow.SharedFlow

/**
 * 사진 이동/복사/삭제 등으로 인해 MediaStore 기반 데이터가 변경되었음을 알리는 단일 신호원입니다.
 *
 * UI/Compose는 직접 데이터를 다시 로드하지 않고,
 * 각 ViewModel이 이 신호를 구독한 뒤 자기 책임 범위의 refresh를 수행합니다.
 */
interface PhotoDataRefreshNotifier {
    val refreshEvents: SharedFlow<RefreshReason>

    suspend fun notify(reason: RefreshReason)
}

enum class RefreshReason {
    CopyCommitted,
    MoveCommitted,
    DeleteCommitted
}

