package com.example.aiautominitest.di

import android.content.Context
import androidx.room.Room
import com.example.aiautominitest.core.IChatEngine
import com.example.aiautominitest.core.NativeChatEngine
import com.example.aiautominitest.data.database.AppDatabase
import com.example.aiautominitest.data.repo.ChatRepository
import com.example.aiautominitest.data.repo.IModelRepository
import com.example.aiautominitest.data.repo.ModelRepositoryImpl

class AppContainer(private val context: Context) {
    
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "ai_chat_database"
        ).build()
    }
    
    val chatRepository: ChatRepository by lazy {
        ChatRepository(database.chatMessageDao())
    }
    
    val modelRepository: IModelRepository by lazy {
        ModelRepositoryImpl(context)
    }
    
    val chatEngine: IChatEngine by lazy {
        NativeChatEngine()
    }
}
