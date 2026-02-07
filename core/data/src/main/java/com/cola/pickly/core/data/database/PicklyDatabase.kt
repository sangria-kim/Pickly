package com.cola.pickly.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// 버전 3: 분석 캐싱 필드 추가 (analysisCriteriaHash, modifiedAt, schemaVersion)
@Database(entities = [PhotoScoreEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PicklyDatabase : RoomDatabase() {
    abstract fun photoScoreDao(): PhotoScoreDao
}