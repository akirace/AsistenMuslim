package com.aghatis.asmal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aghatis.asmal.data.local.dao.MosqueDao
import com.aghatis.asmal.data.local.dao.PrayerDao
import com.aghatis.asmal.data.local.entity.MosqueCacheEntity
import com.aghatis.asmal.data.local.entity.PrayerCacheEntity
import com.aghatis.asmal.data.model.SurahEntity

@Database(
    entities = [SurahEntity::class, PrayerCacheEntity::class, MosqueCacheEntity::class],
    version = 2, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surahDao(): SurahDao
    abstract fun prayerDao(): PrayerDao
    abstract fun mosqueDao(): MosqueDao
}
