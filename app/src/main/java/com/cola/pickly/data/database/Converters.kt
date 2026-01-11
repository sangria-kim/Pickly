package com.cola.pickly.data.database

import android.graphics.Rect
import androidx.room.TypeConverter
import com.cola.pickly.domain.model.RecommendationScore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    // Gson based converters
    private val gson = Gson()

    @TypeConverter
    fun fromRect(rect: Rect?): String? {
        return rect?.let { "${it.left},${it.top},${it.right},${it.bottom}" }
    }

    @TypeConverter
    fun toRect(data: String?): Rect? {
        if (data.isNullOrEmpty()) return null
        val parts = data.split(",")
        return if (parts.size == 4) {
            Rect(parts[0].toInt(), parts[1].toInt(), parts[2].toInt(), parts[3].toInt())
        } else {
            null
        }
    }

    @TypeConverter
    fun fromRectList(list: List<Rect>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    @TypeConverter
    fun toRectList(data: String?): List<Rect>? {
        if (data == null) return null
        val type = object : TypeToken<List<Rect>>() {}.type
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