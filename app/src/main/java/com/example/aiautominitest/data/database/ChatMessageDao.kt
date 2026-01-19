package com.example.aiautominitest.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.aiautominitest.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): ChatMessage?
    
    @Insert
    suspend fun insertMessage(message: ChatMessage): Long
    
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
    
    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(): ChatMessage?
}
