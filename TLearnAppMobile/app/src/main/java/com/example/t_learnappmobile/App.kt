package com.example.t_learnappmobile

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.settings.SettingsManager
import com.google.firebase.FirebaseApp

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("App", "Firebase initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("App", "Firebase init error", e)
        }

        val settingsManager = SettingsManager(this)
        AppCompatDelegate.setDefaultNightMode(settingsManager.getTheme())
        ServiceLocator.initContextAwareDependencies(this)
    }
}