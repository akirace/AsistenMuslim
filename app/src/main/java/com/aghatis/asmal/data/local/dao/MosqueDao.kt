package com.aghatis.asmal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aghatis.asmal.data.local.entity.MosqueCacheEntity

@Dao
interface MosqueDao {
    // Get mosques fetched around a reference point
    // We can't easily query "distance" here without complex math in SQL, 
    // so we rely on the repository logic to check if existing cache is valid for current user location.
    // We just return all and filter in memory or get via reference match.
    // For simplicity: Get all and filter in repo, or get by approximate reference box.
    // Simplest: Get all for now or strict reference match. 
    // Better: Get all, repo checks distance.
    @Query("SELECT * FROM mosque_cache")
    suspend fun getAllMosques(): List<MosqueCacheEntity>
    
    @Query("DELETE FROM mosque_cache")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mosques: List<MosqueCacheEntity>)
}
