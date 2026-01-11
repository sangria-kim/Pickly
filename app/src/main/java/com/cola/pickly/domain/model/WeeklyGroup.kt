package com.cola.pickly.domain.model

import java.time.LocalDate

data class WeeklyGroup(
    val weekId: WeekId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val photos: List<WeeklyPhoto>,
    val recommended: List<WeeklyPhoto>
)