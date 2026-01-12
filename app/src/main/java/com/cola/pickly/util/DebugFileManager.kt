package com.cola.pickly.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

/**
 * 디버깅 정보를 손쉽게 파일로 저장하기 위한 헬퍼.
 *
 * - API 29 이상: MediaStore(Downloads/Documents)를 통해 `Documents/PicklyDebug`에 저장 (추가 권한 불필요)
 * - API 28 이하: 퍼블릭 Documents 디렉터리에 저장 (WRITE_EXTERNAL_STORAGE 권한 필요)
 */
object DebugFileManager {

    /**
     * 디버깅용 텍스트 파일 저장.
     *
     * @param fileName 저장할 파일명 (예: debug-log.txt)
     * @param content  저장할 텍스트
     * @return 저장된 파일의 [Uri] (실패 시 null)
     */
    fun saveDebugText(context: Context, fileName: String, content: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, fileName, content)
            } else {
                saveLegacy(context, fileName, content)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveWithMediaStore(context: Context, fileName: String, content: String): Uri? {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/PicklyDebug")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values) ?: return null

        resolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray())
        } ?: return null

        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, fileName: String, content: String): Uri? {
        val hasWritePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasWritePermission) return null

        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val debugDir = File(documentsDir, "PicklyDebug")
        if (!debugDir.exists() && !debugDir.mkdirs()) {
            return null
        }

        val targetFile = File(debugDir, fileName)
        return try {
            targetFile.writeText(content)
            Uri.fromFile(targetFile)
        } catch (io: IOException) {
            io.printStackTrace()
            null
        }
    }
}
