package com.example.t_learnappmobile.presentation.settings

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.domain.model.Dictionary
import com.example.t_learnappmobile.presentation.components.NotificationManager
import com.example.t_learnappmobile.presentation.theme.*

private const val TAG = "SettingsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    notificationManager: NotificationManager,
    onDictionaryChanged: (String) -> Unit,
    onClose: () -> Unit,
    onLogout: () -> Unit,
    onThemeChanged: (Boolean) -> Unit = {},
    isConnected: Boolean,
    onDataReset: () -> Unit = {}
) {
    Log.d(TAG, "SettingsScreen recompose")

    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showResetDictionaryDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }

    Log.d(TAG, "uiState: isLoading=${uiState.isLoading}, isInitialized=${uiState.isInitialized}, isSuccess=${uiState.isSuccess}")

    DisposableEffect(Unit) {
        Log.d(TAG, "SettingsScreen DisposableEffect - loadData called")
        viewModel.loadData()
        onDispose {
            Log.d(TAG, "SettingsScreen DisposableEffect - onDispose")
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            notificationManager.showSuccess("Операция выполнена успешно")
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            notificationManager.showError(error)
        }
    }

    Scaffold(
        topBar = {
            Log.d(TAG, "SettingsScreen TopAppBar rendering")
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.aeetings),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = {
                        Log.d(TAG, "Close button clicked")
                        onClose()
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && !uiState.isInitialized -> {
                    Log.d(TAG, "Showing full screen loading state")
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = YellowPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Загрузка настроек...", color = MediumGray, fontSize = 14.sp)
                        }
                    }
                }
                else -> {
                    Log.d(TAG, "Showing content state")

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item(key = "profile") {
                                Log.d(TAG, "Rendering ProfileCard")
                                ProfileCard(
                                    firstName = uiState.firstName,
                                    lastName = uiState.lastName,
                                    email = uiState.email,
                                    onEditClick = {
                                        Log.d(TAG, "Edit profile clicked")
                                        if (isConnected) {
                                            showEditProfileDialog = true
                                        } else {
                                            notificationManager.showError("Для редактирования профиля требуется интернет")
                                        }
                                    },
                                    isConnected = isConnected
                                )
                            }

                            item(key = "dictionary") {
                                Log.d(TAG, "Rendering DictionarySelector")
                                DictionarySelector(
                                    dictionaries = uiState.dictionaries,
                                    currentDictionaryId = uiState.currentDictionaryId,
                                    currentDictionaryName = uiState.currentDictionaryName,
                                    onDictionarySelected = { dictId ->
                                        Log.d(TAG, "Dictionary selected: $dictId")
                                        viewModel.updateDictionary(dictId)
                                        onDictionaryChanged(dictId)
                                    }
                                )
                            }

                            item(key = "theme") {
                                Log.d(TAG, "Rendering ThemeSelector")
                                ThemeSelector(
                                    isDarkTheme = uiState.isDarkTheme,
                                    onThemeSelected = { isDark ->
                                        Log.d(TAG, "Theme selected: isDark=$isDark")
                                        viewModel.updateTheme(isDark)
                                        onThemeChanged(isDark)
                                    }
                                )
                            }

                            item(key = "data_management") {
                                Log.d(TAG, "Rendering DataManagement")
                                DataManagement(
                                    onResetDictionary = {
                                        Log.d(TAG, "Reset dictionary clicked")
                                        if (isConnected) {
                                            showResetDictionaryDialog = true
                                        } else {
                                            notificationManager.showError("Для сброса статистики требуется интернет")
                                        }
                                    },
                                    onResetAll = {
                                        Log.d(TAG, "Reset all clicked")
                                        if (isConnected) {
                                            showResetAllDialog = true
                                        } else {
                                            notificationManager.showError("Для сброса всех данных требуется интернет")
                                        }
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                Log.d(TAG, "Logout button clicked")
                                showLogoutDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .align(Alignment.BottomCenter),
                            colors = ButtonDefaults.buttonColors(containerColor = RedError),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Выйти из аккаунта", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        if (uiState.isLoading && uiState.isInitialized) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .clickable(enabled = false) { },
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(120.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = YellowPrimary)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Сохранение...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ФАЙЛ: main/java/com/example/t_learnappmobile/presentation/settings/SettingsScreen.kt
// Часть с диалогом редактирования профиля

    if (showEditProfileDialog) {
        var editFirstName by remember(uiState.firstName) { mutableStateOf(uiState.firstName) }
        var editLastName by remember(uiState.lastName) { mutableStateOf(uiState.lastName) }
        var isSaving by remember { mutableStateOf(false) }

        // Проверка валидности
        val isFirstNameValid = editFirstName.length <= 20
        val isLastNameValid = editLastName.length <= 20
        val isFormValid = isFirstNameValid && isLastNameValid && isConnected && !isSaving

        AlertDialog(
            onDismissRequest = {
                if (!isSaving) showEditProfileDialog = false
            },
            title = { Text("Изменить профиль", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column {
                    // Проверка интернета
                    if (!isConnected) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = RedError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Для изменения профиля требуется интернет",
                                    fontSize = 12.sp,
                                    color = RedError
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = editFirstName,
                        onValueChange = {
                            if (it.length <= 20) editFirstName = it
                        },
                        label = { Text(stringResource(R.string.name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving && isConnected,
                        isError = !isFirstNameValid && editFirstName.isNotEmpty(),
                        supportingText = {
                            if (!isFirstNameValid && editFirstName.isNotEmpty()) {
                                Text(
                                    text = "Максимум 20 символов (сейчас ${editFirstName.length})",
                                    fontSize = 10.sp,
                                    color = RedError
                                )
                            } else if (editFirstName.isNotEmpty()) {
                                Text(
                                    text = "${editFirstName.length}/20",
                                    fontSize = 10.sp,
                                    color = MediumGray
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editLastName,
                        onValueChange = {
                            if (it.length <= 20) editLastName = it
                        },
                        label = { Text(stringResource(R.string.surname)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving && isConnected,
                        isError = !isLastNameValid && editLastName.isNotEmpty(),
                        supportingText = {
                            if (!isLastNameValid && editLastName.isNotEmpty()) {
                                Text(
                                    text = "Максимум 20 символов (сейчас ${editLastName.length})",
                                    fontSize = 10.sp,
                                    color = RedError
                                )
                            } else if (editLastName.isNotEmpty()) {
                                Text(
                                    text = "${editLastName.length}/20",
                                    fontSize = 10.sp,
                                    color = MediumGray
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isConnected && isFormValid) {
                            Log.d(TAG, "Save profile clicked: firstName=$editFirstName, lastName=$editLastName")
                            isSaving = true
                            viewModel.updateProfile(editFirstName, editLastName)
                            showEditProfileDialog = false
                        } else if (!isConnected) {
                            notificationManager.showError("Нет подключения к интернету")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isFormValid
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Сохранение...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Сохранить", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isSaving) showEditProfileDialog = false
                    },
                    enabled = !isSaving
                ) {
                    Text("Отмена", color = BlueColor)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showResetDictionaryDialog) {
        var isResetting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isResetting) showResetDictionaryDialog = false
            },
            title = { Text("Сброс прогресса словаря", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Text(
                    "Весь прогресс изучения слов в текущем словаре будет сброшен. Все слова станут новыми. Игровые очки сохранены.",
                    fontSize = 16.sp,
                    color = MediumGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d(TAG, "Confirm reset dictionary")
                        isResetting = true
                        viewModel.resetDictionaryStatistics {
                            onDataReset()
                            showResetDictionaryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isResetting
                ) {
                    if (isResetting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Сбросить", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isResetting) showResetDictionaryDialog = false
                    },
                    enabled = !isResetting
                ) {
                    Text("Отмена", color = BlueColor)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showResetAllDialog) {
        var isResetting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isResetting) showResetAllDialog = false
            },
            title = { Text("Сброс всех данных", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Text(
                    "Будет сброшен:\n" +
                            "• Прогресс изучения всех слов во всех словарях\n" +
                            "• Все настройки приложения\n\n" +
                            "Будут сохранены:\n" +
                            "• Игровые очки\n" +
                            "• Результаты игр\n" +
                            "• Позиция в таблице лидеров\n\n" +
                            "Это действие нельзя отменить.",
                    fontSize = 16.sp,
                    color = MediumGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d(TAG, "Confirm reset all")
                        isResetting = true
                        viewModel.resetAllData {
                            onDataReset()
                            showResetAllDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isResetting
                ) {
                    if (isResetting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Сбросить", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isResetting) showResetAllDialog = false
                    },
                    enabled = !isResetting
                ) {
                    Text("Отмена", color = BlueColor)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выйти из аккаунта?", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = { Text("Вы уверены, что хотите выйти из аккаунта?", fontSize = 16.sp, color = MediumGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        Log.d(TAG, "Confirm logout")
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
// ФАЙЛ: main/java/com/example/t_learnappmobile/presentation/settings/SettingsScreen.kt
// Обновите ProfileCard, чтобы кнопка редактирования блокировалась без интернета

@Composable
fun ProfileCard(
    firstName: String,
    lastName: String,
    email: String,
    onEditClick: () -> Unit,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.profile),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = BlueColor,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val displayName = buildDisplayName(firstName, lastName)
                    Text(
                        text = displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = email.ifEmpty { "Email не указан" },
                        fontSize = 14.sp,
                        color = MediumGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isConnected) BlueColor else MediumGray
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = isConnected
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isConnected) BlueColor else MediumGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Изменить имя и фамилию" else "Требуется интернет",
                    color = if (isConnected) BlueColor else MediumGray
                )
            }

            if (!isConnected) {
                Text(
                    text = "Для редактирования профиля требуется интернет-соединение",
                    fontSize = 10.sp,
                    color = RedError,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// Вспомогательная функция для сокращения имени
private fun buildDisplayName(firstName: String, lastName: String): String {
    val fullName = if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
        "$firstName $lastName"
    } else if (firstName.isNotEmpty()) {
        firstName
    } else if (lastName.isNotEmpty()) {
        lastName
    } else {
        return "Пользователь"
    }

    return if (fullName.length > 30) {
        fullName.take(27) + "..."
    } else {
        fullName
    }
}

@Composable
fun DictionarySelector(
    dictionaries: List<Dictionary>,
    currentDictionaryId: String,
    currentDictionaryName: String,
    onDictionarySelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.dict), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            var expanded by remember { mutableStateOf(false) }

            val displayName = if (currentDictionaryName.isNotEmpty()) {
                currentDictionaryName
            } else {
                dictionaries.find { it.id == currentDictionaryId }?.name ?: "Выберите словарь"
            }

            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(displayName)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                dictionaries.forEach { dict ->
                    DropdownMenuItem(
                        text = { Text(dict.name) },
                        onClick = {
                            onDictionarySelected(dict.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeSelector(isDarkTheme: Boolean, onThemeSelected: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.design_theme), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    title = stringResource(R.string.light),
                    icon = Icons.Default.LightMode,
                    isSelected = !isDarkTheme,
                    onClick = { onThemeSelected(false) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOption(
                    title = stringResource(R.string.dark),
                    icon = Icons.Default.DarkMode,
                    isSelected = isDarkTheme,
                    onClick = { onThemeSelected(true) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ThemeOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(60.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) YellowPrimary else MaterialTheme.colorScheme.surface
        ),
        elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DataManagement(
    onResetDictionary: () -> Unit,
    onResetAll: () -> Unit
) {
    Column {
        Text(
            stringResource(R.string.data_managment),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.delete_dict), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Сбросить прогресс текущего словаря", fontSize = 12.sp, color = MediumGray)
                }
                OutlinedButton(
                    onClick = onResetDictionary,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.delete_data), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Сбросить прогресс всех словарей", fontSize = 12.sp, color = MediumGray)
                }
                OutlinedButton(
                    onClick = onResetAll,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}