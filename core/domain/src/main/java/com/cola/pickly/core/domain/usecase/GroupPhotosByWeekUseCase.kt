package com.cola.pickly.core.domain.usecase

import com.cola.pickly.core.model.WeekId
import com.cola.pickly.core.model.WeeklyGroup
import com.cola.pickly.core.model.WeeklyPhoto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

class GroupPhotosByWeekUseCase(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val weekFields: WeekFields = WeekFields.of(Locale.getDefault())
) {

    operator fun invoke(photos: List<WeeklyPhoto>): List<WeeklyGroup> {
        if (photos.isEmpty()) return emptyList()

        // 1) 사진마다 LocalDate 계산
        val photosWithDate: List<Pair<WeeklyPhoto, LocalDate>> = photos.map { photo ->
            photo to photo.toLocalDate(zoneId)
        }

        // 2) WeekId 기준으로 그룹핑
        val groupedByWeek: Map<WeekId, List<Pair<WeeklyPhoto, LocalDate>>> =
            photosWithDate.groupBy { (_, date) ->
                val weekBasedYear = date.get(weekFields.weekBasedYear())
                val weekOfYear = date.get(weekFields.weekOfWeekBasedYear())
                WeekId(
                    year = weekBasedYear,
                    weekOfYear = weekOfYear
                )
            }

        // 3) 각 WeekId 그룹을 WeeklyGroup 으로 변환
        val weeklyGroups: List<WeeklyGroup> = groupedByWeek.map { (weekId, photoDatePairs) ->
            val datesInWeek = photoDatePairs.map { it.second }

            // 이 주에 포함된 날짜들 중 가장 최신 날짜를 기준으로 월~일 계산
            val referenceDate: LocalDate = datesInWeek.maxOrNull()!!

            // ISO 기준: 1 = 월요일, 7 = 일요일
            val startDate: LocalDate = referenceDate.with(weekFields.dayOfWeek(), 1)
            val endDate: LocalDate = referenceDate.with(weekFields.dayOfWeek(), 7)

            val photosInWeek: List<WeeklyPhoto> = photoDatePairs
                .map { it.first }
                // 최신 사진이 먼저 오도록 정렬 (원하시면 변경 가능)
                .sortedByDescending { it.takenAt }

            WeeklyGroup(
                weekId = weekId,
                startDate = startDate,
                endDate = endDate,
                photos = photosInWeek,
                recommended = emptyList() // V1에서는 나중 UseCase에서 채움
            )
        }

        // 4) 사진이 한 장도 없는 주는 제외
        val filtered = weeklyGroups.filter { it.photos.isNotEmpty() }

        // 최신 주차가 위로 오도록 정렬
        return filtered.sortedWith(
            compareByDescending<WeeklyGroup> { it.weekId.year }
                .thenByDescending { it.weekId.weekOfYear }
        )
    }

    private fun WeeklyPhoto.toLocalDate(zoneId: ZoneId): LocalDate {
        val instant = Instant.ofEpochMilli(this.takenAt)
        return instant.atZone(zoneId).toLocalDate()
    }
}
