package com.example.t_learnappmobile

import android.app.Application
import com.example.t_learnappmobile.data.repository.ServiceLocator

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initContextAwareDependencies(this)
    }
}