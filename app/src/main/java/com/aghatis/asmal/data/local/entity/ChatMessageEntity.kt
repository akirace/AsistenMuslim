package com.aghatis.asmal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aghatis.asmal.data.model.ChatMessage
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val chatPartner: String // "Deenia AI"
) {
    fun toChatMessage() = ChatMessage(
        id = id,
        text = text,
        isUser = isUser,
        timestamp = timestamp,
        isError = isError
    )

    companion object {
        fun fromChatMessage(message: ChatMessage, chatPartner: String) = ChatMessageEntity(
            id = message.id,
            text = message.text,
            isUser = message.isUser,
            timestamp = message.timestamp,
            isError = message.isError,
            chatPartner = chatPartner
        )
    }
}
