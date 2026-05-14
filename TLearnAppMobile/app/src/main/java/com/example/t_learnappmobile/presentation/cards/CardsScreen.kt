// presentation/cards/CardsScreen.kt
package com.example.t_learnappmobile.presentation.cards

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
import com.example.t_learnappmobile.domain.model.CardType
import com.example.t_learnappmobile.domain.model.Dictionary
import com.example.t_learnappmobile.domain.model.PartOfSpeech
import com.example.t_learnappmobile.domain.model.TranslationDirection
import com.example.t_learnappmobile.domain.model.Word
import com.example.t_learnappmobile.presentation.components.NotificationManager
import com.example.t_learnappmobile.presentation.theme.*

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
// presentation/cards/CardsScreen.kt
// Замените Box с условиями на этот:

    // presentation/cards/CardsScreen.kt
// Замените Box с условиями:

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.showDictionarySelection || uiState.currentDictionary == null) {
            WelcomeScreen(
                dictionaries = uiState.dictionaries.ifEmpty {
                    listOf(
                        Dictionary("finance", "Финансы", 1),
                        Dictionary("conversational", "Разговорные слова", 2),
                        Dictionary("technology", "Технологии", 3),
                        Dictionary("slang", "Сленг", 4)
                    )
                },
                onDictionarySelected = { dictionaryId ->
                    viewModel.selectDictionary(dictionaryId)
                },
                onLogout = onLogout
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                CardsTopBar(
                    dictionaryName = uiState.currentDictionary!!.name,
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

// Новый экран приветствия с кнопками словарей
@Composable
fun WelcomeScreen(
    dictionaries: List<Dictionary>,
    onDictionarySelected: (String) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉 Добро пожаловать!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Выберите словарь для начала изучения:",
            fontSize = 16.sp,
            color = MediumGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Кнопки словарей
        dictionaries.forEach { dictionary ->
            Button(
                onClick = { onDictionarySelected(dictionary.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = dictionary.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = getDictionaryDescription(dictionary.id),
                            fontSize = 12.sp,
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
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка выхода
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedError)
        ) {
            Text("Выйти")
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .widthIn(max = 140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(YellowPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = YellowDark,
                maxLines = 2,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToStatistics, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.Leaderboard, contentDescription = "Статистика", tint = DarkGray, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = onNavigateToGame, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SportsEsports, contentDescription = "Игра", tint = DarkGray, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(56.dp)) {
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
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                if (word.translationDirection == TranslationDirection.EN_TO_RU && word.transcription.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "[${word.transcription}]",
                        fontSize = 20.sp,
                        color = MediumGray,
                        textAlign = TextAlign.Center
                    )
                }

                if (word.partOfSpeech != PartOfSpeech.UNKNOWN) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = word.partOfSpeech.displayName,
                        fontSize = 18.sp,
                        color = DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun EmptyWordsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
            .fillMaxHeight(0.45f)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onNegativeClick,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor = RedError),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Text(negativeText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 24.sp)
            }
        }

        Button(
            onClick = onPositiveClick,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = Color.Black),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Text(positiveText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 24.sp)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 360, heightDp = 720)
@Composable
fun CardsScreenPreview() {
    TLearnAppMobileTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            WelcomeScreen(
                dictionaries = listOf(
                    Dictionary("finance", "Финансы", 1),
                    Dictionary("conversational", "Разговорные слова", 2),
                    Dictionary("technology", "Технологии", 3),
                    Dictionary("slang", "Сленг", 4)
                ),
                onDictionarySelected = {},
                onLogout = {}
            )
        }
    }
}