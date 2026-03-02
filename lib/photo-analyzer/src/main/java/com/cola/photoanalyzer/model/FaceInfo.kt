package com.cola.photoanalyzer.model

data class FaceInfo(
    val count: Int = 0,
    val primaryBox: BoundingBox? = null,
    val allBoxes: List<BoundingBox> = emptyList(),
    val analyzedImageWidth: Int = 0,
    val analyzedImageHeight: Int = 0
)

data class BoundingBox(val left: Int, val top: Int, val right: Int, val bottom: Int)
