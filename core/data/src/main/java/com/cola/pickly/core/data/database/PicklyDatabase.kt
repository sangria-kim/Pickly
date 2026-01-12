package com.cola.pickly.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// 버전 1 -> 2로 증가 (스키마 변경 또는 데이터 초기화를 위해)
@Database(entities = [PhotoScoreEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PicklyDatabase : RoomDatabase() {
    abstract fun photoScoreDao(): PhotoScoreDao
}