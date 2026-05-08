package com.example.t_learnappmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.t_learnappmobile.data.settings.SettingsManager
import com.example.t_learnappmobile.presentation.components.AppNotificationHost
import com.example.t_learnappmobile.presentation.components.rememberNotificationManager
import com.example.t_learnappmobile.presentation.navigation.NavGraph
import com.example.t_learnappmobile.presentation.theme.TLearnAppMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsManager = SettingsManager(this)
        val isDarkTheme = settingsManager.getTheme() == SettingsManager.THEME_DARK

        setContent {
            var darkTheme by remember { mutableStateOf(isDarkTheme) }

            // Слушаем изменения темы
            LaunchedEffect(settingsManager) {
                // Можно добавить listener при необходимости
            }

            TLearnAppMobileTheme(darkTheme = darkTheme) {
                val notificationManager = rememberNotificationManager()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavGraph(
                            notificationManager = notificationManager,
                            isDarkTheme = darkTheme,
                            onThemeChanged = { newDarkTheme ->
                                darkTheme = newDarkTheme
                                val mode = if (newDarkTheme) SettingsManager.THEME_DARK
                                else SettingsManager.THEME_LIGHT
                                settingsManager.setTheme(mode)
                            }
                        )

                        AppNotificationHost(
                            manager = notificationManager,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}