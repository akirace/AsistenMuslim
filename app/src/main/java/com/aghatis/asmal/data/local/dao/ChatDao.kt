package com.aghatis.asmal.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aghatis.asmal.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE chatPartner = :partner ORDER BY timestamp ASC")
    fun getChatHistory(partner: String): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages WHERE chatPartner = :partner")
    suspend fun clearHistory(partner: String)
}
