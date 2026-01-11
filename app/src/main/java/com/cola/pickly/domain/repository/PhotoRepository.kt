package com.cola.pickly.domain.repository

import com.cola.pickly.domain.model.PhotoFolder
import com.cola.pickly.domain.model.WeeklyPhoto

/**
 * 사진 데이터 소스에 접근하기 위한 인터페이스입니다.
 * 데이터 레이어는 이 인터페이스를 구현하여 Domain 레이어에 데이터를 제공합니다.
 */
interface PhotoRepository {

    /**
     * 지정된 폴더 경로에 있는 모든 사진을 가져옵니다.
     * @param folderRelativePath 조회할 폴더의 상대 경로 (예: "DCIM/Camera")
     * @return [WeeklyPhoto]의 리스트. 사진이 없으면 빈 리스트를 반환합니다.
     */
    suspend fun getPhotosInFolder(folderRelativePath: String): List<WeeklyPhoto>

    /**
     * 지정된 Bucket ID에 해당하는 폴더의 모든 사진을 가져옵니다.
     * @param bucketId MediaStore의 BUCKET_ID
     * @return [WeeklyPhoto]의 리스트
     */
    suspend fun getPhotosByBucketId(bucketId: String): List<WeeklyPhoto>

    /**
     * 기본 경로(예: "DCIM/Camera")에 있는 최근 사진들을 가져옵니다.
     * @return [WeeklyPhoto]의 리스트. 사진이 없으면 빈 리스트를 반환합니다.
     */
    suspend fun getRecentPhotos() : List<WeeklyPhoto>

    /**
     * 디바이스에 존재하는 모든 이미지 폴더(Bucket) 목록을 조회합니다.
     * @return [PhotoFolder]의 리스트
     */
    suspend fun getFolders(): List<PhotoFolder>

    /**
     * 디바이스에 존재하는 모든 사진을 가져옵니다.
     * @return [WeeklyPhoto]의 리스트. 사진이 없으면 빈 리스트를 반환합니다.
     */
    suspend fun getAllPhotos(): List<WeeklyPhoto>
}