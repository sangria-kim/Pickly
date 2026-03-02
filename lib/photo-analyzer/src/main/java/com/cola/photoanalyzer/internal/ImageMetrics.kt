package com.cola.photoanalyzer.internal

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.cola.photoanalyzer.detection.DetectedFace
import com.cola.photoanalyzer.model.BoundingBox
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

internal object ImageMetrics {

    @JvmStatic
    fun luminance(pixel: Int): Int {
        return (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
    }

    fun computeLaplacianVariance(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return computeLaplacianVarianceFromPixels(pixels, width, height)
    }

    fun computeLaplacianVariance(bitmap: Bitmap, box: BoundingBox): Double {
        val left = box.left.coerceAtLeast(0)
        val top = box.top.coerceAtLeast(0)
        val right = box.right.coerceAtMost(bitmap.width)
        val bottom = box.bottom.coerceAtMost(bitmap.height)
        val w = right - left
        val h = bottom - top
        if (w <= 2 || h <= 2) return computeLaplacianVariance(bitmap)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, left, top, w, h)
        return computeLaplacianVarianceFromPixels(pixels, w, h)
    }

    private fun computeLaplacianVarianceFromPixels(pixels: IntArray, width: Int, height: Int): Double {
        val gray = IntArray(pixels.size) { luminance(pixels[it]) }
        var mean = 0.0
        val laplacianValues = mutableListOf<Int>()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val laplacian = gray[idx - width] + gray[idx + width] +
                    gray[idx - 1] + gray[idx + 1] - (4 * gray[idx])
                laplacianValues.add(laplacian)
                mean += laplacian
            }
        }
        if (laplacianValues.isEmpty()) return 0.0
        mean /= laplacianValues.size
        var variance = 0.0
        for (v in laplacianValues) {
            variance += (v - mean).pow(2)
        }
        return variance / laplacianValues.size
    }

    fun normalizeSharpness(variance: Double): Double {
        if (variance <= 0.0) return 0.0
        return (ln(1.0 + variance) / ln(1.0 + 2000.0) * 100.0).coerceIn(0.0, 100.0)
    }

    fun calculateLighting(bitmap: Bitmap, box: BoundingBox): Double {
        val left = box.left.coerceAtLeast(0)
        val top = box.top.coerceAtLeast(0)
        val right = box.right.coerceAtMost(bitmap.width)
        val bottom = box.bottom.coerceAtMost(bitmap.height)
        if (left >= right || top >= bottom) return 0.0
        val w = right - left
        val h = bottom - top
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, left, top, w, h)
        var totalLuminance = 0.0
        for (pixel in pixels) {
            totalLuminance += (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel))
        }
        val avgLuminance = totalLuminance / pixels.size
        val dist = abs(avgLuminance - 128)
        return ((1.0 - (dist / 128.0)) * 100).coerceIn(0.0, 100.0)
    }

    fun calculateComposition(bitmap: Bitmap, centerX: Double, centerY: Double): Double {
        val imgWidth = bitmap.width.toDouble()
        val imgHeight = bitmap.height.toDouble()
        val thirdsX = listOf(imgWidth / 3.0, imgWidth * 2.0 / 3.0)
        val thirdsY = listOf(imgHeight / 3.0, imgHeight * 2.0 / 3.0)
        var minDistance = Double.MAX_VALUE
        for (tx in thirdsX) {
            for (ty in thirdsY) {
                val dx = centerX - tx
                val dy = centerY - ty
                val distance = sqrt(dx * dx + dy * dy)
                if (distance < minDistance) minDistance = distance
            }
        }
        val maxDist = sqrt(imgWidth * imgWidth + imgHeight * imgHeight)
        return ((1.0 - (minDistance / (maxDist * 0.5))) * 100).coerceIn(0.0, 100.0)
    }

    fun calculateGroupComposition(bitmap: Bitmap, centers: List<Pair<Double, Double>>): Double {
        if (centers.isEmpty()) return 50.0
        val avgCenterX = centers.map { it.first }.average()
        val avgCenterY = centers.map { it.second }.average()
        return calculateComposition(bitmap, avgCenterX, avgCenterY)
    }

    fun calculateColorContrast(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var sum = 0.0
        var sumSq = 0.0
        for (pixel in pixels) {
            val lum = Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114
            sum += lum
            sumSq += lum * lum
        }
        val n = pixels.size.toDouble()
        val mean = sum / n
        val variance = (sumSq / n) - (mean * mean)
        val stdDev = sqrt(variance.coerceAtLeast(0.0))
        return (stdDev / 80.0 * 100).coerceIn(0.0, 100.0)
    }

    fun calculateLightingUniformity(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val halfW = width / 2
        val halfH = height / 2
        val regions = listOf(
            Rect(0, 0, halfW, halfH),
            Rect(halfW, 0, width, halfH),
            Rect(0, halfH, halfW, height),
            Rect(halfW, halfH, width, height)
        )
        val regionMeans = regions.map { rect ->
            val rw = rect.right - rect.left
            val rh = rect.bottom - rect.top
            if (rw <= 0 || rh <= 0) return@map 128.0
            val pixels = IntArray(rw * rh)
            bitmap.getPixels(pixels, 0, rw, rect.left, rect.top, rw, rh)
            var sum = 0.0
            for (pixel in pixels) {
                sum += Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114
            }
            sum / pixels.size
        }
        val mean = regionMeans.average()
        val variance = regionMeans.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        return ((1.0 - stdDev / 128.0) * 100).coerceIn(0.0, 100.0)
    }

    fun isSunglassesLikely(
        bitmap: Bitmap,
        leftEyeX: Float,
        leftEyeY: Float,
        rightEyeX: Float,
        rightEyeY: Float,
        faceWidth: Int
    ): Boolean {
        val halfSize = (faceWidth * EYE_REGION_RATIO / 2).toInt().coerceAtLeast(1)
        val leftStats = getRegionBrightnessStats(bitmap, leftEyeX.toInt(), leftEyeY.toInt(), halfSize)
            ?: return false
        val rightStats = getRegionBrightnessStats(bitmap, rightEyeX.toInt(), rightEyeY.toInt(), halfSize)
            ?: return false
        return leftStats.first < SUNGLASSES_DARK_THRESHOLD &&
            rightStats.first < SUNGLASSES_DARK_THRESHOLD &&
            leftStats.second < SUNGLASSES_VARIANCE_THRESHOLD &&
            rightStats.second < SUNGLASSES_VARIANCE_THRESHOLD
    }

    /** @return Pair(mean, variance) or null */
    private fun getRegionBrightnessStats(bitmap: Bitmap, cx: Int, cy: Int, halfSize: Int): Pair<Float, Float>? {
        val left = (cx - halfSize).coerceAtLeast(0)
        val top = (cy - halfSize).coerceAtLeast(0)
        val right = (cx + halfSize).coerceAtMost(bitmap.width)
        val bottom = (cy + halfSize).coerceAtMost(bitmap.height)
        val w = right - left
        val h = bottom - top
        if (w <= 0 || h <= 0) return null
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, left, top, w, h)
        var sum = 0f
        for (pixel in pixels) {
            sum += (0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel))
        }
        val mean = sum / pixels.size
        var varianceSum = 0f
        for (pixel in pixels) {
            val lum = 0.299f * Color.red(pixel) + 0.587f * Color.green(pixel) + 0.114f * Color.blue(pixel)
            varianceSum += (lum - mean) * (lum - mean)
        }
        return Pair(mean, varianceSum / pixels.size)
    }

    private const val EYE_REGION_RATIO = 0.15f
    private const val SUNGLASSES_DARK_THRESHOLD = 60f
    private const val SUNGLASSES_VARIANCE_THRESHOLD = 400f
}
