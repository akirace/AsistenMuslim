package com.aghatis.asmal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aghatis.asmal.data.local.entity.PrayerCacheEntity

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_cache WHERE date = :date")
    suspend fun getPrayerByDate(date: String): PrayerCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(prayer: PrayerCacheEntity)
    
    @Query("DELETE FROM prayer_cache WHERE date < :date") // Optional cleanup
    suspend fun clearOldData(date: String)
}
