package com.example.t_learnappmobile

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.settings.SettingsManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val settingsManager = SettingsManager(this)
        AppCompatDelegate.setDefaultNightMode(settingsManager.getTheme())
        ServiceLocator.initContextAwareDependencies(this)
    }
}