package com.example.aiautominitest

import android.app.Application
import com.example.aiautominitest.di.AppContainer
import com.liulishuo.filedownloader.FileDownloader

class App : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        FileDownloader.setup(this)
        container = AppContainer(this)
    }
}
