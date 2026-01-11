package com.cola.pickly.domain.usecase

import com.cola.pickly.domain.model.WeekId
import com.cola.pickly.domain.model.WeeklyGroup
import com.cola.pickly.domain.repository.PhotoRepository

/**
 * 특정 주차(WeekId)에 해당하는 WeeklyGroup 정보를 가져오는 UseCase입니다.
 *
 * 1. 모든 사진을 가져옵니다.
 * 2. 주차별로 그룹핑합니다.
 * 3. 요청된 WeekId에 해당하는 그룹을 찾습니다.
 * 4. 해당 그룹의 사진들 중 2장을 추천합니다.
 * 5. 최종 WeeklyGroup 객체를 반환합니다.
 *
 * @param photoRepository 사진 데이터 소스
 * @param groupPhotosByWeekUseCase 사진을 주차별로 그룹핑하는 UseCase
 * @param pickBestTwoRandomPhotosUseCase 그룹 내에서 2장의 사진을 추천하는 UseCase
 */
class GetWeeklyGroupUseCase(
    private val photoRepository: PhotoRepository,
    private val groupPhotosByWeekUseCase: GroupPhotosByWeekUseCase,
    private val pickBestTwoRandomPhotosUseCase: PickBestTwoRandomPhotosUseCase
) {
    suspend operator fun invoke(weekId: WeekId): WeeklyGroup? {
        // 1 & 2. 전체 사진을 가져와 주별로 그룹핑합니다.
        val allPhotos = photoRepository.getRecentPhotos()
        val allWeeklyGroups = groupPhotosByWeekUseCase(allPhotos)

        // 3. 요청된 WeekId에 해당하는 그룹을 찾습니다.
        val targetGroup = allWeeklyGroups.find { it.weekId == weekId }

        return if (targetGroup != null) {
            // 4. 찾은 그룹에 대해 2장의 사진을 추천하고 결과를 반환합니다.
            // pickBestTwoRandomPhotosUseCase는 리스트를 받아 리스트를 반환하므로,
            // 단일 객체를 리스트로 감싸 호출하고 결과의 첫 번째 요소를 사용합니다.
            pickBestTwoRandomPhotosUseCase(listOf(targetGroup)).firstOrNull()
        } else {
            null
        }
    }
}