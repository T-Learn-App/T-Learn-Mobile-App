package com.example.t_learnappmobile.presentation.cards

import android.R.attr.padding
import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Dictionary
import com.example.t_learnappmobile.model.PartOfSpeech
import com.example.t_learnappmobile.model.TranslationDirection
import com.example.t_learnappmobile.model.Word
import com.example.t_learnappmobile.presentation.components.NotificationManager
import com.example.t_learnappmobile.presentation.components.rememberNotificationManager
import com.example.t_learnappmobile.presentation.theme.*

// ==================== ГЛАВНЫЙ ЭКРАН ====================

// Файл: presentation/cards/CardsScreen.kt
// Добавьте этот код в существующий файл

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    viewModel: CardsViewModel,
    notificationManager: NotificationManager,
    onNavigateToGame: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error -> notificationManager.showError(error) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ДОБАВЛЕНО: Показываем экран выбора словаря для новых пользователей
        if (uiState.showDictionarySelection) {
            DictionarySelectionScreen(
                dictionaries = uiState.dictionaries,
                onDictionarySelected = { dictionaryId ->
                    viewModel.selectDictionary(dictionaryId)
                },
                onNavigateToSettings = onNavigateToSettings,
                onLogout = onLogout
            )
        } else {
            // Существующий экран карточек
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                CardsTopBar(
                    dictionaryName = uiState.currentDictionary?.name ?: "Словарь",
                    onNavigateToStatistics = onNavigateToStatistics,
                    onNavigateToGame = onNavigateToGame,
                    onNavigateToSettings = onNavigateToSettings,
                    onLogout = onLogout
                )

                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.isLoading -> LoadingView()
                        uiState.currentWord != null -> WordCardSection(
                            word = uiState.currentWord!!,
                            isTranslationHidden = uiState.isTranslationHidden,
                            cardType = viewModel.getCardType(),
                            onToggleTranslation = { viewModel.toggleTranslation() }
                        )
                        else -> EmptyWordsView()
                    }
                }

                if (uiState.currentWord != null && !uiState.isLoading) {
                    val (positiveBtn, negativeBtn) = viewModel.getButtonTexts()
                    ActionButtons(
                        positiveText = positiveBtn,
                        negativeText = negativeBtn,
                        onPositiveClick = { viewModel.onKnowCard() },
                        onNegativeClick = { viewModel.onDontKnowCard() }
                    )
                }
            }
        }
    }
}

// ДОБАВЛЕНО: Новый экран выбора словаря
@Composable
fun DictionarySelectionScreen(
    dictionaries: List<Dictionary>,
    onDictionarySelected: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Приветственный текст
        Text(
            text = "🎉 Добро пожаловать!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Выберите словарь для начала изучения:",
            fontSize = 16.sp,
            color = MediumGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Карточки словарей
        dictionaries.forEach { dictionary ->
            DictionaryCard(
                dictionary = dictionary,
                onClick = { onDictionarySelected(dictionary.id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопки навигации
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Настройки")
            }

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)
            ) {
                Text("Выйти")
            }
        }
    }
}

@Composable
fun DictionaryCard(
    dictionary: Dictionary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dictionary.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Описание словаря (можно добавить в модель Dictionary)
                Text(
                    text = getDictionaryDescription(dictionary.id),
                    fontSize = 14.sp,
                    color = MediumGray
                )
            }

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Выбрать",
                tint = YellowPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


fun getDictionaryDescription(dictionaryId: String): String {
    return when (dictionaryId) {
        "finance" -> "Финансовые термины и выражения"
        "conversational" -> "Повседневные разговорные слова"
        "technology" -> "IT и технологические термины"
        "slang" -> "Современный сленг и выражения"
        else -> "Изучайте новые слова"
    }
}

// ==================== КОМПОНЕНТЫ ====================
@Composable
fun CardsTopBar(
    dictionaryName: String,
    onNavigateToStatistics: () -> Unit,
    onNavigateToGame: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dictionaryName,
                fontSize = 16.sp, // ← Уменьшен шрифт чтобы помещалось
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .widthIn(max = 140.dp) // ← Ограничение максимальной ширины
                    .clip(RoundedCornerShape(12.dp))
                    .background(YellowPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = YellowDark,
                maxLines = 2, // ← Разрешить перенос на 2 строки
                textAlign = TextAlign.Center,
                lineHeight = 18.sp // ← Межстрочный интервал
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToStatistics,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Filled.Leaderboard, contentDescription = "Статистика", tint = DarkGray, modifier = Modifier.size(32.dp))
                }
                IconButton(
                    onClick = onNavigateToGame,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Filled.SportsEsports, contentDescription = "Игра", tint = DarkGray, modifier = Modifier.size(32.dp))
                }
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = "Настройки", tint = DarkGray, modifier = Modifier.size(32.dp))
                }
            }
        }
    }

    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
}
@Composable
fun WordCardSection(
    word: Word,
    isTranslationHidden: Boolean,
    cardType: CardType,
    onToggleTranslation: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // ← ДОБАВЬТЕ ПРОКРУТКУ
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Тип карточки
                Surface(
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(16.dp),
                    color = when (cardType) {
                        CardType.NEW -> BlueColor.copy(alpha = 0.15f)
                        CardType.ROTATION -> YellowPrimary.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = when (cardType) {
                            CardType.NEW -> "Новое слово"
                            CardType.ROTATION -> "Этап ${word.stage}/7"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (cardType == CardType.NEW) BlueColor else YellowDark,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Основное слово
                Text(
                    text = when (word.translationDirection) {
                        TranslationDirection.EN_TO_RU -> word.englishWord
                        TranslationDirection.RU_TO_EN -> word.translation
                    },
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 42.sp
                )

                // Транскрипция
                if (word.translationDirection == TranslationDirection.EN_TO_RU && word.transcription.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "[${word.transcription}]",
                        fontSize = 20.sp,
                        color = MediumGray,
                        textAlign = TextAlign.Center
                    )
                }

                // Часть речи
                if (word.partOfSpeech != PartOfSpeech.UNKNOWN) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = word.partOfSpeech.russian,
                        fontSize = 18.sp,
                        color = DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопка показать/скрыть перевод
                TextButton(
                    onClick = onToggleTranslation,
                    colors = ButtonDefaults.textButtonColors(contentColor = BlueColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isTranslationHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTranslationHidden) "Показать перевод" else "Скрыть перевод",
                        fontSize = 18.sp
                    )
                }

                // Перевод
                AnimatedVisibility(
                    visible = !isTranslationHidden,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = YellowPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = when (word.translationDirection) {
                                TranslationDirection.EN_TO_RU -> word.translation
                                TranslationDirection.RU_TO_EN -> word.englishWord
                            },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Дополнительный отступ снизу для скролла
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
@Composable
fun EmptyWordsView() {
    Column(
        modifier = Modifier
            .fillMaxSize() // ← Измените с fillMaxWidth на fillMaxSize
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // ← Это центрирует по вертикали
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = BlueColor,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Все слова выучены! 🎉",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Выберите другой словарь или сыграйте в игру",
            fontSize = 16.sp,
            color = MediumGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}
@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = YellowPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Загрузка слов...", color = MediumGray, fontSize = 16.sp)
        }
    }
}
@Composable
fun ActionButtons(
    positiveText: String,
    negativeText: String,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.45f) // Увеличено с 0.33f до 0.45f - почти половина экрана
            .padding(horizontal = 12.dp, vertical = 12.dp), // Увеличены отступы
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onNegativeClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor = RedError),
            shape = RoundedCornerShape(24.dp), // Более скругленные углы
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp, // Больше тень
                pressedElevation = 10.dp
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp), // Увеличена иконка с 36 до 48
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp)) // Увеличен отступ
                Text(
                    negativeText,
                    fontSize = 20.sp, // Увеличен шрифт с 18 до 20
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 24.sp
                )
            }
        }

        Button(
            onClick = onPositiveClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(
                containerColor = YellowPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(24.dp), // Более скругленные углы
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 6.dp, // Больше тень
                pressedElevation = 10.dp
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp), // Увеличена иконка с 36 до 48
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp)) // Увеличен отступ
                Text(
                    positiveText,
                    fontSize = 18.sp, // Увеличен шрифт с 18 до 20
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

// ==================== PREVIEW ====================

@Preview(showBackground = true, showSystemUi = true, widthDp = 360, heightDp = 720)
@Composable
fun CardsScreenPreview() {
    TLearnAppMobileTheme {
        // Напрямую отрисовываем UI с тестовыми данными
        var isTranslationHidden by remember { mutableStateOf(true) }
        val testWord = Word(
            id = "1",
            englishWord = "investment",
            translation = "инвестиция",
            transcription = "ɪnˈvestmənt",
            partOfSpeech = PartOfSpeech.NOUN,
            stage = 0,
            isNew = true,
            translationDirection = TranslationDirection.EN_TO_RU
        )
        val testDictionaries = listOf(
            Dictionary("finance", "Финансы", 1),
            Dictionary("conversational", "Разговорные слова", 2),
            Dictionary("technology", "Технологии", 3),
            Dictionary("slang", "Сленг", 4)
        )

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 4.dp), verticalArrangement = Arrangement.SpaceBetween) {
                CardsTopBar(
                    dictionaryName = "Финансы",
                    onNavigateToStatistics = {},
                    onNavigateToGame = {},
                    onNavigateToSettings = {},
                    onLogout = {}
                )

                WordCardSection(
                    word = testWord,
                    isTranslationHidden = isTranslationHidden,
                    cardType = CardType.NEW,
                    onToggleTranslation = { isTranslationHidden = !isTranslationHidden }
                )

                ActionButtons(
                    positiveText = "Я знаю это слово",
                    negativeText = "Я не знаю это слово",
                    onPositiveClick = {},
                    onNegativeClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CardsScreenDarkPreview() {
    TLearnAppMobileTheme(darkTheme = true) {
        val testWord = Word(
            id = "1",
            englishWord = "investment",
            translation = "инвестиция",
            transcription = "ɪnˈvestmənt",
            partOfSpeech = PartOfSpeech.NOUN,
            stage = 3,
            isNew = false,
            translationDirection = TranslationDirection.RU_TO_EN
        )

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                CardsTopBar(
                    dictionaryName = "Технологии",
                    onNavigateToStatistics = {},
                    onNavigateToGame = {},
                    onNavigateToSettings = {},
                    onLogout = {}
                )

                WordCardSection(
                    word = testWord,
                    isTranslationHidden = false,
                    cardType = CardType.ROTATION,
                    onToggleTranslation = {}
                )

                ActionButtons(
                    positiveText = "Я запомнил",
                    negativeText = "Я не запомнил",
                    onPositiveClick = {},
                    onNegativeClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Empty Words")
@Composable
fun CardsScreenEmptyPreview() {
    TLearnAppMobileTheme {
        val testDictionaries = listOf(
            Dictionary("finance", "Финансы", 1),
            Dictionary("technology", "Технологии", 2)
        )

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                CardsTopBar(
                    dictionaryName = "Финансы",
                    onNavigateToStatistics = {},
                    onNavigateToGame = {},
                    onNavigateToSettings = {},
                    onLogout = {}
                )

                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    EmptyWordsView(

                    )
                }
            }
        }
    }
}