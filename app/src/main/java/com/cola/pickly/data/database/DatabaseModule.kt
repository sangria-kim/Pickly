package com.cola.pickly.data.database

import android.content.Context
import androidx.room.Room

object DatabaseModule {
    private var database: PicklyDatabase? = null

    fun getDatabase(context: Context): PicklyDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                PicklyDatabase::class.java,
                "pickly_db"
            )
            .fallbackToDestructiveMigration() // 버전 불일치 시 기존 데이터 삭제 후 재생성
            .build()
            database = instance
            instance
        }
    }
}