package com.cola.photoanalyzer.detection.mlkit

import android.graphics.Bitmap
import com.cola.photoanalyzer.detection.DetectedFace
import com.cola.photoanalyzer.detection.FaceDetector
import com.cola.photoanalyzer.detection.Point
import com.cola.photoanalyzer.model.BoundingBox
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitFaceDetector : FaceDetector {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setMinFaceSize(0.01f)
        .build()

    private val detector = FaceDetection.getClient(options)

    override suspend fun detect(bitmap: Bitmap): List<DetectedFace> {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            detector.process(image)
                .addOnSuccessListener { faces ->
                    cont.resume(faces.map { face ->
                        val box = face.boundingBox
                        DetectedFace(
                            boundingBox = BoundingBox(box.left, box.top, box.right, box.bottom),
                            leftEyeOpenProb = face.leftEyeOpenProbability,
                            rightEyeOpenProb = face.rightEyeOpenProbability,
                            smileProb = face.smilingProbability,
                            hasNose = face.getLandmark(FaceLandmark.NOSE_BASE) != null,
                            hasMouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT) != null,
                            hasMouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT) != null,
                            hasMouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null,
                            leftEyePosition = face.getLandmark(FaceLandmark.LEFT_EYE)?.position?.let {
                                Point(it.x, it.y)
                            },
                            rightEyePosition = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.let {
                                Point(it.x, it.y)
                            }
                        )
                    })
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }
}
