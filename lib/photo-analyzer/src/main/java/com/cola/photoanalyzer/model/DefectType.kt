package com.cola.photoanalyzer.model

/** 사진 결함 유형 (reject 사유) */
enum class DefectType(val label: String) {
    NO_FACE("No face"),
    BLURRY("Blurry"),
    TOO_SMALL("Face too small"),
    OCCLUDED("Face occluded"),
    EYES_CLOSED("Eyes closed"),
    CROPPED("Face cropped")
}
