package com.example.aiautominitest.di

import android.content.Context
import com.example.aiautominitest.core.IChatEngine
import com.example.aiautominitest.core.NativeChatEngine
import com.example.aiautominitest.data.repo.IModelRepository
import com.example.aiautominitest.data.repo.ModelRepositoryImpl

class AppContainer(private val context: Context) {
    val modelRepository: IModelRepository by lazy {
        ModelRepositoryImpl(context)
    }
    
    val chatEngine: IChatEngine by lazy {
        NativeChatEngine()
    }
}
