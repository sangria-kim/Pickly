package com.cola.pickly.core.data.photo

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
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
        photoIds.map { id ->
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        }
    }

    override suspend fun movePhotos(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport = withContext(Dispatchers.IO) {
        val destPath = normalizeRelativePath(destinationRelativePath)
        var success = 0
        var skipped = 0
        var failed = 0
        val errors = mutableListOf<String>()

        photoIds.forEach { id ->
            val info = queryPhotoInfo(id)
            if (info == null) {
                failed++
                errors.add("정보 조회 실패: $id")
                return@forEach
            }

            val targetName = resolveTargetName(destPath, info.displayName, policy)
            if (targetName == null) {
                skipped++
                return@forEach
            }

            val inserted = insertDestUri(destPath, targetName, info.mimeType)
            if (inserted == null) {
                failed++
                errors.add("삽입 실패: $targetName")
                return@forEach
            }

            try {
                copyStream(info.uri, inserted)
                // 원본 삭제
                contentResolver.delete(info.uri, null, null)
                success++
            } catch (e: Exception) {
                // 실패 시 생성된 항목 정리
                contentResolver.delete(inserted, null, null)
                failed++
                errors.add(e.message ?: "이동 실패: $targetName")
            }
        }

        PhotoActionReport(
            successCount = success,
            skippedCount = skipped,
            failedCount = failed,
            errors = errors
        )
    }

    override suspend fun copyPhotos(
        photoIds: List<Long>,
        destinationRelativePath: String,
        policy: DuplicateFilenamePolicy
    ): PhotoActionReport = withContext(Dispatchers.IO) {
        val destPath = normalizeRelativePath(destinationRelativePath)
        var success = 0
        var skipped = 0
        var failed = 0
        val errors = mutableListOf<String>()

        photoIds.forEach { id ->
            val info = queryPhotoInfo(id)
            if (info == null) {
                failed++
                errors.add("정보 조회 실패: $id")
                return@forEach
            }

            val targetName = resolveTargetName(destPath, info.displayName, policy)
            if (targetName == null) {
                skipped++
                return@forEach
            }

            val inserted = insertDestUri(destPath, targetName, info.mimeType)
            if (inserted == null) {
                failed++
                errors.add("삽입 실패: $targetName")
                return@forEach
            }

            try {
                copyStream(info.uri, inserted)
                success++
            } catch (e: Exception) {
                contentResolver.delete(inserted, null, null)
                failed++
                errors.add(e.message ?: "복사 실패: $targetName")
            }
        }

        PhotoActionReport(
            successCount = success,
            skippedCount = skipped,
            failedCount = failed,
            errors = errors
        )
    }

    override suspend fun softDeletePhotos(photoIds: List<Long>): PhotoActionReport = withContext(Dispatchers.IO) {
        var success = 0
        var failed = 0
        val errors = mutableListOf<String>()

        photoIds.forEach { id ->
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
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

    private fun queryPhotoInfo(id: Long): PhotoInfo? {
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        val projection = arrayOf(
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
            val pathIdx = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)

            if (cursor.moveToFirst()) {
                val name = cursor.getString(nameIdx) ?: return null
                val mime = cursor.getString(mimeIdx) ?: "image/*"
                val relative = cursor.getString(pathIdx)
                return PhotoInfo(uri, name, mime, relative)
            }
        }
        return null
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
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
        }
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun copyStream(from: Uri, to: Uri) {
        val input = contentResolver.openInputStream(from) ?: error("입력 스트림 없음")
        val output = contentResolver.openOutputStream(to) ?: error("출력 스트림 없음")

        input.use { src ->
            output.use { dst ->
                src.copyTo(dst)
            }
        }
    }

    private fun findExisting(relativePath: String, displayName: String): Uri? {
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} = ?"
        val args = arrayOf(relativePath, displayName)
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            selection,
            args,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idIdx)
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
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
}
