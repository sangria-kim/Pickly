package com.cola.pickly.data.photo

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.cola.pickly.domain.model.PhotoFolder
import com.cola.pickly.domain.model.WeeklyPhoto
import com.cola.pickly.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MediaStore를 사용하여 기기에 저장된 사진을 가져오는 [PhotoRepository]의 구현체입니다.
 */
@Singleton
class MediaStorePhotoRepository @Inject constructor(
    private val contentResolver: ContentResolver
) : PhotoRepository {

    override suspend fun getPhotosInFolder(
        folderRelativePath: String
    ): List<WeeklyPhoto> = withContext(Dispatchers.IO) {
        // 기존 구현과 동일하게 DATA 또는 RELATIVE_PATH LIKE 검색
        // 다만 getPhotosByBucketId가 더 정확하므로 보통은 BucketId 사용을 권장.
        // 여기서는 기존 로직 유지.
        val photos = mutableListOf<WeeklyPhoto>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Images.Media.RELATIVE_PATH)
        }

        val (selection, selectionArgs) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?" to arrayOf("%$folderRelativePath%")
            } else {
                "${MediaStore.Images.Media.DATA} LIKE ?" to arrayOf("%/$folderRelativePath/%")
            }

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        queryPhotos(collection, projection.toTypedArray(), selection, selectionArgs, sortOrder, photos)
        photos
    }

    override suspend fun getPhotosByBucketId(bucketId: String): List<WeeklyPhoto> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<WeeklyPhoto>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val selection = "${MediaStore.Images.Media.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(bucketId)
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        queryPhotos(collection, projection, selection, selectionArgs, sortOrder, photos)
        photos
    }

    override suspend fun getRecentPhotos(): List<WeeklyPhoto> =
        getPhotosInFolder(DEFAULT_RELATIVE_PATH)

    override suspend fun getFolders(): List<PhotoFolder> = withContext(Dispatchers.IO) {
        val folderMap = mutableMapOf<String, PhotoFolder>()

        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdCol) ?: continue
                val bucketName = cursor.getString(bucketNameCol) ?: "Unknown"
                val photoId = cursor.getLong(idCol)

                if (folderMap.containsKey(bucketId)) {
                    val current = folderMap[bucketId]!!
                    folderMap[bucketId] = current.copy(count = current.count + 1)
                } else {
                    val thumbnailUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        photoId
                    ).toString()

                    folderMap[bucketId] = PhotoFolder(
                        id = bucketId,
                        name = bucketName,
                        count = 1,
                        thumbnailUri = thumbnailUri
                    )
                }
            }
        }
        folderMap.values.toList().sortedBy { it.name }
    }

    override suspend fun getAllPhotos(): List<WeeklyPhoto> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<WeeklyPhoto>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        queryPhotos(collection, projection, null, null, sortOrder, photos)
        photos
    }

    // 공통 쿼리 로직 추출
    private fun queryPhotos(
        collection: android.net.Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String,
        destination: MutableList<WeeklyPhoto>
    ) {
        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateTakenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
            val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val filePath = cursor.getString(dataColumn)

                val takenAtMillis = when {
                    dateTakenColumn != -1 && !cursor.isNull(dateTakenColumn) -> {
                        cursor.getLong(dateTakenColumn)
                    }
                    dateAddedColumn != -1 && !cursor.isNull(dateAddedColumn) -> {
                        cursor.getLong(dateAddedColumn) * 1000L
                    }
                    else -> 0L
                }

                val width = if (widthColumn != -1 && !cursor.isNull(widthColumn)) cursor.getInt(widthColumn) else null
                val height = if (heightColumn != -1 && !cursor.isNull(heightColumn)) cursor.getInt(heightColumn) else null

                destination.add(
                    WeeklyPhoto(
                        id = id,
                        filePath = filePath,
                        takenAt = takenAtMillis,
                        width = width,
                        height = height
                    )
                )
            }
        }
    }

    companion object {
        const val DEFAULT_RELATIVE_PATH = "DCIM/Camera"
    }
}
