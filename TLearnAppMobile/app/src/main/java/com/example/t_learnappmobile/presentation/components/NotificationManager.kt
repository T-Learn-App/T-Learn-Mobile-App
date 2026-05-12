// presentation/components/NotificationManager.kt
package com.example.t_learnappmobile.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NotificationManager {
    private val _message = mutableStateOf<String?>(null)
    val message: String? get() = _message.value
    private var scope: CoroutineScope? = null

    fun init(scope: CoroutineScope) {
        this.scope = scope
    }

    fun show(text: String, duration: Long = 2000) {
        _message.value = text
        scope?.launch {
            kotlinx.coroutines.delay(duration)
            _message.value = null
        }
    }

    fun showError(text: String) = show(text)
    fun showSuccess(text: String) = show(text)
}

@Composable
fun rememberNotificationManager(): NotificationManager {
    val manager = remember { NotificationManager() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(scope) {
        manager.init(scope)
    }
    return manager
}

@Composable
fun AppNotificationHost(
    manager: NotificationManager,
    modifier: Modifier = Modifier
) {
    manager.message?.let { message ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.contains("error", ignoreCase = true))
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}