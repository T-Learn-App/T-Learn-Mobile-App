// App.kt
package com.example.t_learnappmobile

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.t_learnappmobile.di.AppModule
import com.google.firebase.FirebaseApp

class App : Application() {

    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("App", "Firebase initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("App", "Firebase init error", e)
        }

        // Initialize AppModule (Dependency Injection)
        appModule = AppModule(this)

        // Set initial theme
        val theme = appModule.settingsLocalSource.getTheme()
        AppCompatDelegate.setDefaultNightMode(theme)

        // Start periodic sync
        appModule.syncManager.startPeriodicSync()

        Log.d("App", "Application initialized successfully")
    }
}