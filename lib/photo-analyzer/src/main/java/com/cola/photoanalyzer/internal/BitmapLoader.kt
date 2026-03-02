package com.cola.photoanalyzer.internal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Size
import androidx.exifinterface.media.ExifInterface

object BitmapLoader {

    /**
     * 파일 경로에서 비트맵을 로드합니다 (EXIF 회전 적용, 리사이징).
     * @return (회전된 비트맵, EXIF 반영 원본 크기) 또는 실패 시 null
     */
    fun loadBitmap(path: String, maxSize: Int = 1024): Pair<Bitmap, Size>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        if (originalWidth <= 0 || originalHeight <= 0) return null

        var sampleSize = 1
        while (options.outWidth / sampleSize > maxSize || options.outHeight / sampleSize > maxSize) {
            sampleSize *= 2
        }

        val bitmap = BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return null

        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val rotatedBitmap = if (orientation != ExifInterface.ORIENTATION_NORMAL &&
            orientation != ExifInterface.ORIENTATION_UNDEFINED
        ) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
        val rotatedOriginalSize = if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270
        ) {
            Size(originalHeight, originalWidth)
        } else {
            Size(originalWidth, originalHeight)
        }
        return Pair(rotatedBitmap, rotatedOriginalSize)
    }

    /**
     * dHash/버스트 감지용 썸네일 로드 (EXIF 회전 적용).
     */
    fun loadThumbnail(path: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        val sampleSize = maxOf(
            options.outWidth / (targetWidth * 4),
            options.outHeight / (targetHeight * 4),
            1
        )
        val bitmap = BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        ) ?: return null

        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        val rotated = if (orientation != ExifInterface.ORIENTATION_NORMAL &&
            orientation != ExifInterface.ORIENTATION_UNDEFINED
        ) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val scaled = Bitmap.createScaledBitmap(rotated, targetWidth, targetHeight, true)
        if (scaled != rotated) rotated.recycle()
        return scaled
    }
}
