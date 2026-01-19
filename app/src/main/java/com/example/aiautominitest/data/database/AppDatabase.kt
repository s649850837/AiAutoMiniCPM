package com.example.aiautominitest.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aiautominitest.data.model.ChatMessage

@Database(entities = [ChatMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
}
