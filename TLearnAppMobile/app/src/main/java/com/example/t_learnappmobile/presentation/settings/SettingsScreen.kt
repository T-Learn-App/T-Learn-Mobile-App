package com.example.t_learnappmobile.presentation.settings

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.presentation.components.NotificationManager
import com.example.t_learnappmobile.presentation.components.rememberNotificationManager
import com.example.t_learnappmobile.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    notificationManager: NotificationManager,
    onDictionaryChanged: (String) -> Unit,
    onClose: () -> Unit,
    onLogout: () -> Unit,
    onThemeChanged: (Boolean) -> Unit = {} // ← Новый параметр
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error -> notificationManager.showError(error) }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) notificationManager.showSuccess("Профиль обновлен")
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp), // ← Опущен экран
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.aeetings),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Выбор словаря
            item {
                DictionarySelector(
                    dictionaries = uiState.dictionaries,
                    currentDictionaryId = uiState.currentDictionaryId,
                    onDictionarySelected = { dictId ->
                        viewModel.updateDictionary(dictId)
                        onDictionaryChanged(dictId)
                    }
                )
            }

            // Выбор темы
            item {
                ThemeSelector(
                    isDarkTheme = uiState.isDarkTheme,
                    onThemeSelected = { isDark ->
                        viewModel.updateTheme(isDark)
                        onThemeChanged(isDark) // ← Уведомляем об изменении темы
                    }
                )
            }

            // Редактор профиля
            item {
                ProfileEditor(
                    firstName = uiState.firstName,
                    lastName = uiState.lastName,
                    onSave = { firstName, lastName ->
                        viewModel.updateProfile(firstName, lastName)
                    }
                )
            }

            // Управление данными
            item {
                DataManagement(
                    onResetDictionary = { viewModel.resetDictionaryStatistics() },
                    onResetAll = { viewModel.resetAllData() }
                )
            }

            // Кнопка выхода из аккаунта
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedError
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Выйти из аккаунта",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Индикатор загрузки
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Диалог ошибки
        uiState.error?.let { error ->
            Dialog(onDismissRequest = { viewModel.clearError() }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ошибка", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedError)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.clearError() }) { Text("OK") }
                    }
                }
            }
        }
    }

    // Диалог подтверждения выхода
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Выйти из аккаунта?", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            },
            text = {
                Text("Вы уверены, что хотите выйти из аккаунта?", fontSize = 16.sp, color = MediumGray)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Выйти", color = RedError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Отмена", color = BlueColor)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// Остальные компоненты остаются без изменений
@Composable
fun DictionarySelector(
    dictionaries: List<com.example.t_learnappmobile.model.Dictionary>,
    currentDictionaryId: String,
    onDictionarySelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.dict), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            var expanded by remember { mutableStateOf(false) }
            val currentDict = dictionaries.find { it.id == currentDictionaryId }

            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(currentDict?.name ?: "Выберите словарь")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    dictionaries.forEach { dict ->
                        DropdownMenuItem(
                            text = { Text(dict.name) },
                            onClick = { onDictionarySelected(dict.id); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelector(isDarkTheme: Boolean, onThemeSelected: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.design_theme), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOption(stringResource(R.string.light), Icons.Default.LightMode, !isDarkTheme, { onThemeSelected(false) }, Modifier.weight(1f))
                ThemeOption(stringResource(R.string.dark), Icons.Default.DarkMode, isDarkTheme, { onThemeSelected(true) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ThemeOption(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(60.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) YellowPrimary else MaterialTheme.colorScheme.background),
        elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = title, tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ProfileEditor(firstName: String, lastName: String, onSave: (String, String) -> Unit) {
    var localFirstName by remember(firstName) { mutableStateOf(firstName) }
    var localLastName by remember(lastName) { mutableStateOf(lastName) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.profile), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = localFirstName, onValueChange = { localFirstName = it }, label = { Text(stringResource(R.string.name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = localLastName, onValueChange = { localLastName = it }, label = { Text(stringResource(R.string.surname)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onSave(localFirstName, localLastName) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = Color.Black)
            ) { Text(stringResource(R.string.save_profile)) }
        }
    }
}

@Composable
fun DataManagement(onResetDictionary: () -> Unit, onResetAll: () -> Unit) {
    var showDictionaryDialog by remember { mutableStateOf(false) }
    var showAllDialog by remember { mutableStateOf(false) }

    Column {
        Text(stringResource(R.string.data_managment), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.delete_dict), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.delete_stats_or_current_dict), fontSize = 12.sp, color = MediumGray)
                }
                OutlinedButton(onClick = { showDictionaryDialog = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)) {
                    Text(stringResource(R.string.delete))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.delete_data), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.delete_data_and_progress), fontSize = 12.sp, color = MediumGray)
                }
                OutlinedButton(onClick = { showAllDialog = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)) {
                    Text(stringResource(R.string.delete))
                }
            }
        }

        if (showDictionaryDialog) {
            AlertDialog(
                onDismissRequest = { showDictionaryDialog = false },
                title = { Text("Сброс статистики категории") },
                text = { Text("Весь прогресс изучения слов в текущей категории будет безвозвратно удален.") },
                confirmButton = { TextButton(onClick = { onResetDictionary(); showDictionaryDialog = false }) { Text("Да, удалить", color = RedError) } },
                dismissButton = { TextButton(onClick = { showDictionaryDialog = false }) { Text("Отмена") } }
            )
        }

        if (showAllDialog) {
            AlertDialog(
                onDismissRequest = { showAllDialog = false },
                title = { Text("Сброс всех данных") },
                text = { Text("Будут удалены:\n• Прогресс изучения всех слов\n• Результаты игр\n• Все настройки приложения\n\nЭто действие нельзя отменить.") },
                confirmButton = { TextButton(onClick = { onResetAll(); showAllDialog = false }) { Text("Да, удалить", color = RedError) } },
                dismissButton = { TextButton(onClick = { showAllDialog = false }) { Text("Отмена") } }
            )
        }
    }
}

// Превью
@Preview(showBackground = true, showSystemUi = true, widthDp = 360, heightDp = 720)
@Composable
fun SettingsScreenPreview() {
    TLearnAppMobileTheme {
        SettingsScreen(
            viewModel = remember { SettingsViewModel() },
            notificationManager = rememberNotificationManager(),
            onDictionaryChanged = {},
            onClose = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsScreenDarkPreview() {
    TLearnAppMobileTheme(darkTheme = true) {
        SettingsScreen(
            viewModel = remember { SettingsViewModel() },
            notificationManager = rememberNotificationManager(),
            onDictionaryChanged = {},
            onClose = {},
            onLogout = {}
        )
    }
}