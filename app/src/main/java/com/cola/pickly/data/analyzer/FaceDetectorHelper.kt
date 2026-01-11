package com.cola.pickly.data.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google ML Kit Face Detection 기능을 래핑한 헬퍼 클래스입니다.
 * 얼굴 감지, 랜드마크 인식, 웃음/눈 상태 분류 등을 수행합니다.
 */
class FaceDetectorHelper {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // 정확도 우선
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)            // 눈, 코, 입 위치
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // 눈 뜸, 웃음 확률
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setMinFaceSize(0.01f) // 최소 얼굴 크기를 1%로 설정하여 작은 얼굴도 일단 모두 검출
        .build()

    private val detector: FaceDetector = FaceDetection.getClient(options)

    /**
     * Uri로부터 이미지를 로드하여 얼굴을 감지합니다.
     */
    suspend fun detectFaces(context: Context, imageUri: Uri): List<Face> {
        return try {
            val image = InputImage.fromFilePath(context, imageUri)
            detectFaces(image)
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Bitmap으로부터 얼굴을 감지합니다.
     */
    suspend fun detectFaces(bitmap: Bitmap): List<Face> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return detectFaces(image)
    }

    private suspend fun detectFaces(image: InputImage): List<Face> = suspendCancellableCoroutine { cont ->
        detector.process(image)
            .addOnSuccessListener { faces ->
                cont.resume(faces)
            }
            .addOnFailureListener { e ->
                // 에러 발생 시 빈 리스트 반환 또는 예외 던지기
                // 여기서는 분석 실패로 간주하고 예외를 전달
                cont.resumeWithException(e)
            }
    }
}