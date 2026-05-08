package com.example.t_learnappmobile.presentation.game

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.presentation.components.NotificationManager
import com.example.t_learnappmobile.presentation.components.rememberNotificationManager
import com.example.t_learnappmobile.presentation.theme.*
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    notificationManager: NotificationManager,
    onGameFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error -> notificationManager.showError(error) }
    }

    LaunchedEffect(Unit) {
        viewModel.startGame()
    }

    // Сбрасываем selectedAnswer при загрузке нового слова
    LaunchedEffect(uiState.currentWord) {
        selectedAnswer = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> GameLoadingView()
            uiState.error != null && !uiState.isLoading -> GameErrorView(uiState.error!!, onGameFinished)
            uiState.isGameActive && uiState.currentWord != null -> ActiveGameView(
                score = uiState.score,
                currentWord = uiState.currentWord!!.english,
                currentIndex = uiState.currentWordIndex,
                totalWords = uiState.totalWords,
                options = uiState.options,
                correctIndex = uiState.correctOptionIndex,
                selectedAnswer = selectedAnswer,
                onOptionClick = { index ->
                    if (selectedAnswer == null) {
                        selectedAnswer = index
                        viewModel.selectAnswer(index)
                    }
                },
                onExitClick = { showExitDialog = true }
            )
            uiState.showResults -> GameResultsView(
                score = uiState.score,
                totalWords = uiState.totalWords,
                onClose = {
                    viewModel.closeResults()
                    onGameFinished()
                }
            )
        }
    }

    // Диалог подтверждения выхода
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Выйти из игры?", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = { Text("Вы уверены, что хотите выйти? Текущий прогресс будет потерян.", fontSize = 16.sp, color = MediumGray) },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    viewModel.closeResults()
                    onGameFinished()
                }) { Text("Выйти", color = RedError, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Продолжить", color = BlueColor) } },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun GameLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = YellowPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Загрузка игры...", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun GameErrorView(error: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = error, textAlign = TextAlign.Center, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Закрыть", fontSize = 16.sp) }
    }
}

@Composable
fun ActiveGameView(
    score: Int,
    currentWord: String,
    currentIndex: Int,
    totalWords: Int,
    options: List<String>,
    correctIndex: Int,
    selectedAnswer: Int?,
    onOptionClick: (Int) -> Unit,
    onExitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 36.dp)
    ) {
        // Верхняя панель
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$score очков",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = YellowPrimary
            )
            IconButton(onClick = onExitClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Выйти",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Карточка слова
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.4f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Выберите перевод:",
                        fontSize = 14.sp,
                        color = MediumGray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = currentWord,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "${currentIndex + 1}/$totalWords",
                        fontSize = 16.sp,
                        color = MediumGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Кнопки вариантов
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedAnswer == index
                val isCorrect = selectedAnswer != null && index == correctIndex
                val isWrong = selectedAnswer == index && index != correctIndex

                val borderColor = when {
                    isCorrect -> Color(0xFF4CAF50) // Зеленый для правильного
                    isWrong -> RedError // Красный для неправильного
                    else -> Color.Transparent
                }

                val borderWidth = if (isCorrect || isWrong) 3.dp else 0.dp

                Button(
                    onClick = {
                        if (selectedAnswer == null) onOptionClick(index)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .border(borderWidth, borderColor, RoundedCornerShape(20.dp)), // ← Обводка
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isCorrect -> Color(0xFF4CAF50)
                            isWrong -> RedError
                            else -> YellowPrimary
                        },
                        contentColor = when {
                            isCorrect || isWrong -> Color.White
                            else -> Color.Black
                        }
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp
                    )
                ) {
                    Text(
                        text = option,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isCorrect || isWrong -> Color.White
                            else -> Color.Black
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GameResultsView(
    score: Int,
    totalWords: Int,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("$score очков!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = YellowPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("$totalWords слов завершено", fontSize = 16.sp, color = MediumGray)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("ЗАКРЫТЬ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}

// ==================== PREVIEW ====================

@Preview(showBackground = true, showSystemUi = true, widthDp = 360, heightDp = 720)
@Composable
fun GameScreenPreview() {
    TLearnAppMobileTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ActiveGameView(
                score = 500,
                currentWord = "investment",
                currentIndex = 5,
                totalWords = 10,
                options = listOf("инвестиция", "расход"),
                correctIndex = 0,
                selectedAnswer = null,
                onOptionClick = {},
                onExitClick = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Game - Correct Answer")
@Composable
fun GameScreenCorrectPreview() {
    TLearnAppMobileTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ActiveGameView(
                score = 600,
                currentWord = "investment",
                currentIndex = 5,
                totalWords = 10,
                options = listOf("инвестиция", "расход"),
                correctIndex = 0,
                selectedAnswer = 0, // Правильный ответ
                onOptionClick = {},
                onExitClick = {}
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Game - Wrong Answer")
@Composable
fun GameScreenWrongPreview() {
    TLearnAppMobileTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ActiveGameView(
                score = 500,
                currentWord = "investment",
                currentIndex = 5,
                totalWords = 10,
                options = listOf("инвестиция", "расход"),
                correctIndex = 0,
                selectedAnswer = 1, // Неправильный ответ
                onOptionClick = {},
                onExitClick = {}
            )
        }
    }
}