package com.cola.pickly.domain.usecase

import com.cola.pickly.domain.model.WeeklyGroup
import com.cola.pickly.domain.model.WeeklyPhoto
import kotlin.random.Random

/**
 * V1: 각 주(WeeklyGroup)에서 사진을 최대 2장 랜덤으로 추천 리스트에 채워주는 UseCase.
 *
 * - photos 가 0장  → recommended = 빈 리스트
 * - photos 가 1장  → recommended = 그 1장
 * - photos 가 2장+ → recommended = 랜덤 2장
 */
class PickBestTwoRandomPhotosUseCase(
    private val random: Random = Random.Default
) {

    operator fun invoke(groups: List<WeeklyGroup>): List<WeeklyGroup> {
        return groups.map { group ->
            val recommended = pickUpToTwoRandom(group.photos)
            group.copy(recommended = recommended)
        }
    }

    private fun pickUpToTwoRandom(photos: List<WeeklyPhoto>): List<WeeklyPhoto> {
        return when {
            photos.isEmpty() -> emptyList()
            photos.size == 1 -> listOf(photos[0])
            else -> photos.shuffled(random).take(2)
        }
    }
}