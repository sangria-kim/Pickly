package com.cola.pickly.core.data.photo

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.cola.pickly.core.data.settings.DuplicateFilenamePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStorePhotoActionRepository @Inject constructor(
    private val contentResolver: ContentResolver
) : PhotoActionRepository {

    override suspend fun getShareUris(photoIds: List<Long>): List<Uri> = withContext(Dispatchers.IO) {
        val collection = imagesCollection()
        photoIds.map { id ->
            ContentUris.withAppendedId(collection, id)
        }
    }

    override suspend fun createWriteRequestIntentSender(photoIds: List<Long>): IntentSender? = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty()) return@withContext null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext null

        val collection = imagesCollection()
        val uris = photoIds.map { id ->
            ContentUris.withAppendedId(collection, id)
        }
        MediaStore.createWriteRequest(contentResolver, uris).intentSender
    }

    override suspend fun createDeleteRequestIntentSender(photoIds: List<Long>): IntentSender? = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty()) return@withContext null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext null

        val collection = imagesCollection()
        val uris = photoIds.map { id ->
            ContentUris.withAppendedId(collection, id)
        }
        MediaStore.createDeleteRequest(contentResolver, uris).intentSender
    }

    override suspend fun movePhotos(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport = withContext(Dispatchers.IO) {
        // Android 11+ (API 30+)에서는 RELATIVE_PATH 직접 업데이트를 먼저 시도
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val updateReport = tryMoveByUpdate(photoIds, destinationRelativePath, policy)

            // 실패한 항목만 copy+delete 전략으로 폴백
            val fallbackIds = updateReport.failedIds
            if (fallbackIds.isNotEmpty()) {
                val fallbackReport = movePhotosByCopyDelete(fallbackIds, destinationRelativePath, policy)
                return@withContext mergeReports(updateReport, fallbackReport)
            }

            return@withContext updateReport
        } else {
            // Android 10 이하에서는 copy+delete 전략만 사용
            return@withContext movePhotosByCopyDelete(photoIds, destinationRelativePath, policy)
        }
    }

    private suspend fun tryMoveByUpdate(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport = withContext(Dispatchers.IO) {
        val destPath = normalizeRelativePath(destinationRelativePath)
        val collection = imagesCollection()
        var success = 0
        var skipped = 0
        var failed = 0
        val errors = mutableListOf<String>()
        val successIds = mutableListOf<Long>()
        val skippedIds = mutableListOf<Long>()
        val failedIds = mutableListOf<Long>()

        photoIds.forEach { id ->
            val info = queryPhotoInfo(id)
            if (info == null) {
                failed++
                errors.add("정보 조회 실패: $id")
                failedIds.add(id)
                return@forEach
            }

            // 중복 확인 및 정책 적용
            val targetName = resolveTargetName(destPath, info.displayName, policy)
            if (targetName == null) {
                skipped++
                skippedIds.add(id)
                return@forEach
            }

            try {
                val uri = ContentUris.withAppendedId(collection, id)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.RELATIVE_PATH, destPath)
                    if (targetName != info.displayName) {
                        put(MediaStore.Images.Media.DISPLAY_NAME, targetName)
                    }
                }

                val affected = contentResolver.update(uri, values, null, null)
                if (affected > 0) {
                    success++
                    successIds.add(id)
                } else {
                    failed++
                    failedIds.add(id)
                    errors.add("업데이트 실패: $id")
                }
            } catch (e: SecurityException) {
                // 권한 부족 시 폴백 대상으로 처리
                failed++
                failedIds.add(id)
                errors.add("권한 부족: $id")
            } catch (e: Exception) {
                failed++
                failedIds.add(id)
                errors.add("${e.javaClass.simpleName}: ${e.message ?: "업데이트 실패: $id"}")
            }
        }

        PhotoActionReport(
            successCount = success,
            skippedCount = skipped,
            failedCount = failed,
            errors = errors,
            successIds = successIds,
            skippedIds = skippedIds,
            failedIds = failedIds
        )
    }

    private suspend fun movePhotosByCopyDelete(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport = withContext(Dispatchers.IO) {
        val destPath = normalizeRelativePath(destinationRelativePath)
        val collection = imagesCollection()
        var success = 0
        var skipped = 0
        var failed = 0
        val errors = mutableListOf<String>()
        val successIds = mutableListOf<Long>()
        val skippedIds = mutableListOf<Long>()
        val failedIds = mutableListOf<Long>()

        photoIds.forEach { id ->
            val info = queryPhotoInfo(id)
            if (info == null) {
                failed++
                val errorMsg = "정보 조회 실패: $id (collection=$collection)"
                errors.add(errorMsg)
                failedIds.add(id)
                return@forEach
            }

            val targetName = resolveTargetName(destPath, info.displayName, policy)
            if (targetName == null) {
                skipped++
                skippedIds.add(id)
                return@forEach
            }

            val inserted = insertDestUri(destPath, targetName, info.mimeType)
            if (inserted == null) {
                failed++
                val errorMsg = "삽입 실패: $targetName"
                errors.add(errorMsg)
                failedIds.add(id)
                return@forEach
            }

            try {
                copyStream(info.uri, inserted)

                // 주의: createDeleteRequest(IntentSender) 승인 후 시스템이 실제 삭제를 수행할 수 있으므로,
                // '이동'에서는 여기서 원본을 delete 하지 않습니다.
                success++
                successIds.add(id)
            } catch (e: Exception) {
                // 실패 시 생성된 항목 정리
                val cleaned = runCatching {
                    val result = contentResolver.delete(inserted, null, null)
                    result
                }.getOrNull()
                failed++
                failedIds.add(id)
                errors.add("${e.javaClass.simpleName}: ${e.message ?: "이동 실패: $targetName"}")
            }
        }

        PhotoActionReport(
            successCount = success,
            skippedCount = skipped,
            failedCount = failed,
            errors = errors,
            successIds = successIds,
            skippedIds = skippedIds,
            failedIds = failedIds
        )
    }

    private fun mergeReports(first: PhotoActionReport, second: PhotoActionReport): PhotoActionReport {
        // 1. 폴백으로 성공한 항목 식별
        val fallbackSuccessIds = first.failedIds.intersect(second.successIds.toSet())

        // 2. 최종 successIds 계산 (중복 제거)
        val finalSuccessIds = (first.successIds + second.successIds).distinct()

        // 3. 최종 failedIds 계산
        //    - first의 실패 중 폴백으로 성공하지 못한 것들
        //    - second의 실패 (폴백에서도 실패)
        val finalFailedIds = (first.failedIds - fallbackSuccessIds) + second.failedIds

        // 4. 최종 skippedIds 계산 (중복 제거)
        val finalSkippedIds = (first.skippedIds + second.skippedIds).distinct()

        // 5. 에러 메시지 필터링
        //    - 에러 메시지 포맷: "설명: ID" (예: "권한 부족: 123")
        //    - 폴백으로 성공한 ID의 에러는 제거
        val finalErrors = first.errors.filter { error ->
            val failedIdInError = error.substringAfterLast(": ", "").toLongOrNull()
            failedIdInError == null || failedIdInError !in fallbackSuccessIds
        } + second.errors

        return PhotoActionReport(
            successCount = finalSuccessIds.size,
            skippedCount = finalSkippedIds.size,
            failedCount = finalFailedIds.size,
            errors = finalErrors,
            successIds = finalSuccessIds,
            skippedIds = finalSkippedIds,
            failedIds = finalFailedIds
        )
    }

    override suspend fun copyPhotos(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport = withContext(Dispatchers.IO) {
        val destPath = normalizeRelativePath(destinationRelativePath)
        val collection = imagesCollection()
        var success = 0
        var skipped = 0
        var failed = 0
        val errors = mutableListOf<String>()
        val successIds = mutableListOf<Long>()
        val skippedIds = mutableListOf<Long>()
        val failedIds = mutableListOf<Long>()

        photoIds.forEach { id ->
            val info = queryPhotoInfo(id)
            if (info == null) {
                failed++
                errors.add("정보 조회 실패: $id (collection=$collection)")
                failedIds.add(id)
                return@forEach
            }

            val targetName = resolveTargetName(destPath, info.displayName, policy)
            if (targetName == null) {
                skipped++
                skippedIds.add(id)
                return@forEach
            }

            val inserted = insertDestUri(destPath, targetName, info.mimeType)
            if (inserted == null) {
                failed++
                errors.add("삽입 실패: $targetName")
                failedIds.add(id)
                return@forEach
            }

            try {
                copyStream(info.uri, inserted)
                success++
                successIds.add(id)
            } catch (e: Exception) {
                val cleaned = runCatching { contentResolver.delete(inserted, null, null) }.getOrNull()
                failed++
                failedIds.add(id)
                errors.add(e.message ?: "복사 실패: $targetName")
            }
        }

        PhotoActionReport(
            successCount = success,
            skippedCount = skipped,
            failedCount = failed,
            errors = errors,
            successIds = successIds,
            skippedIds = skippedIds,
            failedIds = failedIds
        )
    }

    override suspend fun softDeletePhotos(photoIds: List<Long>): PhotoActionReport = withContext(Dispatchers.IO) {
        var success = 0
        var failed = 0
        val errors = mutableListOf<String>()
        val collection = imagesCollection()

        photoIds.forEach { id ->
            val uri = ContentUris.withAppendedId(collection, id)
            try {
                val affected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_TRASHED, 1)
                    }
                    contentResolver.update(uri, values, null, null)
                } else {
                    contentResolver.delete(uri, null, null)
                }

                if (affected > 0) {
                    success++
                } else {
                    failed++
                    errors.add("삭제 실패: $id")
                }
            } catch (e: SecurityException) {
                failed++
                errors.add("권한 부족: $id")
            } catch (e: Exception) {
                failed++
                errors.add(e.message ?: "삭제 실패: $id")
            }
        }

        PhotoActionReport(
            successCount = success,
            failedCount = failed,
            errors = errors
        )
    }

    private fun normalizeRelativePath(path: String): String =
        if (path.endsWith("/")) path else "$path/"

    private fun imagesCollection(): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    private fun queryPhotoInfo(id: Long): PhotoInfo? {
        // 우선 Images 컬렉션에서 조회
        val imagesCollection = imagesCollection()
        queryPhotoInfoFromCollection(id, imagesCollection)?.let { return it }

        // 실패 시 Files 컬렉션(이미지 타입)에서 조회
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val filesCollection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            queryPhotoInfoFromFilesCollection(id, filesCollection)?.let { return it }
        }

        // 최종적으로 internal 볼륨도 시도
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val internal = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_INTERNAL)
            queryPhotoInfoFromCollection(id, internal)?.let { return it }
        }

        return null
    }

    private fun queryPhotoInfoFromCollection(id: Long, collection: Uri): PhotoInfo? {
        val uri = ContentUris.withAppendedId(collection, id)

        val projection = buildList {
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Images.Media._ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        return try {
            contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return null
                }

                val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val pathIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                val name = cursor.getString(nameIdx)
                if (name == null) {
                    return null
                }
                
                val mime = cursor.getString(mimeIdx) ?: "image/*"
                val relative = if (pathIdx != -1 && !cursor.isNull(pathIdx)) cursor.getString(pathIdx) else null

                PhotoInfo(uri, name, mime, relative)
            } ?: run {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun queryPhotoInfoFromFilesCollection(id: Long, filesCollection: Uri): PhotoInfo? {
        val uri = ContentUris.withAppendedId(filesCollection, id)

        val projection = buildList {
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Files.FileColumns.RELATIVE_PATH)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Files.FileColumns._ID} = ? AND ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}"
        val selectionArgs = arrayOf(id.toString())

        return try {
            contentResolver.query(filesCollection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return null
                }

                val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val pathIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
                } else {
                    -1
                }

                val name = cursor.getString(nameIdx)
                if (name == null) {
                    return null
                }
                
                val mime = cursor.getString(mimeIdx) ?: "image/*"
                val relative = if (pathIdx != -1 && !cursor.isNull(pathIdx)) cursor.getString(pathIdx) else null

                // Files 컬렉션에서 찾았으므로 Images 컬렉션 URI로 변환
                val imagesCollection = imagesCollection()
                val imagesUri = ContentUris.withAppendedId(imagesCollection, id)

                PhotoInfo(imagesUri, name, mime, relative)
            } ?: run {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveTargetName(
        destinationRelativePath: String,
        displayName: String,
        policy: DuplicateFilenamePolicy
    ): String? {
        val existing = findExisting(destinationRelativePath, displayName)
        if (existing == null) return displayName

        return when (policy) {
            DuplicateFilenamePolicy.Overwrite -> {
                contentResolver.delete(existing, null, null)
                displayName
            }
            DuplicateFilenamePolicy.Skip -> null
        }
    }

    private fun insertDestUri(relativePath: String, displayName: String, mimeType: String): Uri? {
        val collection = imagesCollection()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        return try {
            val inserted = contentResolver.insert(collection, values)
            inserted
        } catch (e: Exception) {
            null
        }
    }

    private fun copyStream(from: Uri, to: Uri) {
        val input = contentResolver.openInputStream(from) ?: error("입력 스트림 없음: $from")
        val output = contentResolver.openOutputStream(to) ?: error("출력 스트림 없음: $to")

        input.use { src ->
            output.use { dst ->
                src.copyTo(dst)
            }
        }

        // Q+에서 IS_PENDING을 0으로 업데이트하여 갤러리 반영 트리거
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(to, values, null, null)
            }
        }
    }

    private fun findExisting(relativePath: String, displayName: String): Uri? {
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val args = arrayOf(relativePath, displayName)
        val collection = imagesCollection()
        contentResolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID),
            selection,
            args,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idIdx)
                return ContentUris.withAppendedId(collection, id)
            }
        }
        return null
    }

    private data class PhotoInfo(
        val uri: Uri,
        val displayName: String,
        val mimeType: String,
        val relativePath: String?
    )

    private companion object {
        // 실패 분석을 위한 최소 로그만 유지합니다(실패 시점/예외 원인 파악용).
        private const val TAG = "MediaStorePhotoActionRepo"
    }
}
