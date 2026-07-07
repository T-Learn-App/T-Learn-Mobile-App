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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.t_learnappmobile.data.local.SettingsLocalSource
import com.example.t_learnappmobile.presentation.components.AppNotificationHost
import com.example.t_learnappmobile.presentation.components.rememberNotificationManager
import com.example.t_learnappmobile.presentation.navigation.NavGraph
import com.example.t_learnappmobile.presentation.theme.TLearnAppMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appModule = (application as App).appModule
        val networkMonitor = (application as App).networkMonitor
        val themeMode = appModule.settingsLocalSource.getTheme()
        val isDarkTheme = themeMode == SettingsLocalSource.THEME_DARK

        setContent {
            var darkTheme by remember { mutableStateOf(isDarkTheme) }
            val isConnected by networkMonitor.isConnected.collectAsStateWithLifecycle(initialValue = true)

            TLearnAppMobileTheme(darkTheme = darkTheme) {
                val notificationManager = rememberNotificationManager()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavGraph(
                            notificationManager = notificationManager,
                            onThemeChanged = { newDarkTheme ->
                                darkTheme = newDarkTheme
                                val mode = if (newDarkTheme) {
                                    SettingsLocalSource.THEME_DARK
                                } else {
                                    SettingsLocalSource.THEME_LIGHT
                                }
                                appModule.settingsLocalSource.setTheme(mode)
                            },
                            appModule = appModule,
                            isConnected = isConnected
                        )
                        AppNotificationHost(manager = notificationManager)
                    }
                }
            }
        }
    }
}