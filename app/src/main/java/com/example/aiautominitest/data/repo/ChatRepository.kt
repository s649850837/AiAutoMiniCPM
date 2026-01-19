package com.example.aiautominitest.data.repo

import com.example.aiautominitest.data.database.ChatMessageDao
import com.example.aiautominitest.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: ChatMessageDao) {
    
    fun getAllMessages(): Flow<List<ChatMessage>> = dao.getAllMessages()
    
    suspend fun insertMessage(message: ChatMessage): Long {
        return dao.insertMessage(message)
    }



    suspend fun getLastMessage(): ChatMessage? {
        return dao.getLastMessage()
    }
    
    suspend fun clearHistory() {
        dao.deleteAllMessages()
    }
}
