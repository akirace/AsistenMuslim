package com.aghatis.asmal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aghatis.asmal.data.model.SurahEntity

@Database(entities = [SurahEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surahDao(): SurahDao
}
