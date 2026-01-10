package com.aghatis.asmal.data.repository

import com.aghatis.asmal.data.local.dao.ChatDao
import com.aghatis.asmal.data.local.entity.ChatMessageEntity
import com.aghatis.asmal.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(private val chatDao: ChatDao) {
    fun getChatHistory(partner: String): Flow<List<ChatMessage>> {
        return chatDao.getChatHistory(partner).map { entities ->
            entities.map { it.toChatMessage() }
        }
    }

    suspend fun saveMessage(message: ChatMessage, partner: String) {
        chatDao.insertMessage(ChatMessageEntity.fromChatMessage(message, partner))
    }

    suspend fun clearHistory(partner: String) {
        chatDao.clearHistory(partner)
    }
}
