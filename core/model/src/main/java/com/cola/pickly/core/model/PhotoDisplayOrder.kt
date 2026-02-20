package com.cola.pickly.core.model

/**
 * 사진 표시 순서의 단일 계약.
 *
 * 1) 촬영 시각 최신순(takenAt DESC)
 * 2) 촬영 시각이 같으면 ID 내림차순(id DESC)으로 tie-break
 */
val PhotoDisplayOrderComparator: Comparator<Photo> =
    compareByDescending<Photo> { it.takenAt }
        .thenByDescending { it.id }
