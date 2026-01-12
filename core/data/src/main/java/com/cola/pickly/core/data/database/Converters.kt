package com.cola.pickly.core.data.database

import androidx.room.TypeConverter
import com.cola.pickly.core.model.FaceBoundingBox
import com.cola.pickly.core.model.RecommendationScore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    // Gson based converters
    private val gson = Gson()

    @TypeConverter
    fun fromFaceBoundingBox(box: FaceBoundingBox?): String? {
        return box?.let { "${it.left},${it.top},${it.right},${it.bottom}" }
    }

    @TypeConverter
    fun toFaceBoundingBox(data: String?): FaceBoundingBox? {
        if (data.isNullOrEmpty()) return null
        val parts = data.split(",")
        return if (parts.size == 4) {
            FaceBoundingBox(
                left = parts[0].toInt(),
                top = parts[1].toInt(),
                right = parts[2].toInt(),
                bottom = parts[3].toInt()
            )
        } else {
            null
        }
    }

    @TypeConverter
    fun fromFaceBoundingBoxList(list: List<FaceBoundingBox>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    @TypeConverter
    fun toFaceBoundingBoxList(data: String?): List<FaceBoundingBox>? {
        if (data == null) return null
        val type = object : TypeToken<List<FaceBoundingBox>>() {}.type
        return gson.fromJson(data, type)
    }

    @TypeConverter
    fun fromScore(score: RecommendationScore?): String? {
        return score?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toScore(data: String?): RecommendationScore? {
        if (data == null) return null
        return gson.fromJson(data, RecommendationScore::class.java)
    }
}