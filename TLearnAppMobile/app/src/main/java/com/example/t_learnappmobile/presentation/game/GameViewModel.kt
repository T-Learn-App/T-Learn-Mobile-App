package com.example.t_learnappmobile.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.model.GameMode
import com.example.t_learnappmobile.domain.model.GameWord
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    private var gameWords: List<GameWord> = emptyList()
    private var timerJob: Job? = null
    private var currentWordIndex = 0

    fun startGame(mode: GameMode) {
        viewModelScope.launch {
            val words = ServiceLocator.wordRepository.getNewWords() +
                    ServiceLocator.wordRepository.getRotationWords()

            gameWords = words.shuffled().take(15).map {
                GameWord(it.id, it.englishWord, it.russianTranslation)
            }

            currentWordIndex = 0
            _uiState.value = GameState(
                gameMode = mode,
                isGameActive = true,
                totalWords = gameWords.size,
                wordsLeft = gameWords.size
            )

            loadNextWord()
            startTimer(mode)
        }
    }

    private fun startTimer(mode: GameMode) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            if (mode == GameMode.TIME) {
                var time = 120
                while (time > 0 && _uiState.value.isGameActive && isActive) {
                    _uiState.value = _uiState.value.copy(timer = time)
                    delay(1000)
                    time--
                }
                endGame()
            }
        }
    }

    fun selectAnswer(selectedIndex: Int) {
        val state = _uiState.value
        if (!state.isGameActive || state.currentWord == null) return

        val points = if (selectedIndex == state.correctOptionIndex) 100 else 0
        val newScore = state.score + points

        viewModelScope.launch {
            if (currentWordIndex + 1 < gameWords.size) {
                currentWordIndex++
                _uiState.value = state.copy(score = newScore)
                loadNextWord()
            } else {
                endGame(newScore)
            }
        }
    }

    private suspend fun loadNextWord() {
        if (currentWordIndex >= gameWords.size) {
            endGame()
            return
        }

        val word = gameWords[currentWordIndex]
        val wrongAnswer = getRandomWrongAnswer(word.russian)
        val options = listOf(word.russian, wrongAnswer).shuffled()
        val correctIndex = options.indexOf(word.russian)

        _uiState.value = _uiState.value.copy(
            currentWord = word,
            options = options,
            correctOptionIndex = correctIndex,
            currentWordIndex = currentWordIndex + 1,
            wordsLeft = gameWords.size - (currentWordIndex + 1)
        )
    }

    private fun getRandomWrongAnswer(correct: String): String {
        val wrongAnswers = listOf(
            "Дом", "Кот", "Собака", "Машина", "Чай", "Кофе", "Книга",
            "Стол", "Окно", "Дверь", "Река", "Гора", "Дерево", "Небо"
        )
        return wrongAnswers.filter { it != correct }.random()
    }

    private suspend fun endGame(finalScore: Int = _uiState.value.score) {
        timerJob?.cancel()
        val userId = ServiceLocator.tokenManager.getUserData().firstOrNull()?.id ?: 1
        // Сохраняем результат в БД для лидерборда
        viewModelScope.launch {
            // ServiceLocator.gameResultDao.insert(GameResultEntity(...))
        }
        _uiState.value = _uiState.value.copy(
            isGameActive = false,
            score = finalScore
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
