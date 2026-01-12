package com.example.aiautominitest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aiautominitest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        binding.tvStatus.text = "Hello Project Initialized"
    }
}
