package com.aghatis.asmal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aghatis.asmal.data.model.QoriEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QoriDao {
    @Query("SELECT * FROM qori")
    fun getAllQoris(): Flow<List<QoriEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(qoris: List<QoriEntity>)

    @Query("SELECT COUNT(*) FROM qori")
    suspend fun getCount(): Int

    @Query("DELETE FROM qori")
    suspend fun deleteAll()
}
