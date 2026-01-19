package com.example.aiautominitest

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aiautominitest.databinding.ActivityMainBinding
import com.example.aiautominitest.ui.chat.ChatActivityWithVoice

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Directly launch ChatActivityWithVoice (supports voice input/output)
        startActivity(Intent(this, ChatActivityWithVoice::class.java))
        finish()
    }
}

