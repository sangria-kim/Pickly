package com.cola.pickly.core.model

data class PhotoFolder(
    val id: String,
    val name: String,
    val count: Int,
    val thumbnailUri: String,
    val relativePath: String
)
