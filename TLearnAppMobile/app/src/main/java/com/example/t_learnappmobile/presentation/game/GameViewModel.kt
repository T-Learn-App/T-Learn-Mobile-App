package com.example.t_learnappmobile.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.game.GameResultEntity
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
    private var allWordsForWrongAnswers: List<String> = emptyList() // пул русских слов

    fun startGame(mode: GameMode) {
        viewModelScope.launch {
            // Загружаем реальные слова из репозитория
            val newWords = ServiceLocator.wordRepository.getNewWords()
            val rotationWords = ServiceLocator.wordRepository.getRotationWords()

            // ✅ КОНВЕРТАЦИЯ в GameWord
            gameWords = (newWords + rotationWords)
                .map { word ->
                    GameWord(word.id, word.englishWord, word.russianTranslation)
                }
                .shuffled()
                .take(15)

            // Собираем все русские переводы для неправильных ответов
            allWordsForWrongAnswers = (newWords + rotationWords)
                .map { it.russianTranslation }

            currentWordIndex = 0
            _uiState.value = GameState(
                gameMode = mode,
                isGameActive = true,
                totalWords = gameWords.size,
                wordsLeft = if (mode == GameMode.WORDS) 15 else 0
            )

            loadNextWord()
            startTimer(mode)
        }
    }


    private fun startTimer(mode: GameMode) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            when (mode) {
                GameMode.TIME -> {
                    var time = 120
                    while (time > 0 && _uiState.value.isGameActive && isActive) {
                        _uiState.value = _uiState.value.copy(timer = time)
                        delay(1000)
                        time--
                    }
                    endGame()
                }
                GameMode.WORDS -> {
                    // Логика по словам - таймер не нужен
                }
            }
        }
    }

    fun selectAnswer(selectedIndex: Int) {
        val state = _uiState.value
        if (!state.isGameActive || state.currentWord == null) return

        val isCorrect = selectedIndex == state.correctOptionIndex
        val points = if (isCorrect) 100 else 0
        val newScore = state.score + points

        viewModelScope.launch {
            if (currentWordIndex + 1 < gameWords.size &&
                (state.gameMode == GameMode.TIME || state.wordsLeft > 1)) {

                currentWordIndex++
                _uiState.value = state.copy(
                    score = newScore,
                    wordsLeft = state.wordsLeft - 1
                )
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
        return allWordsForWrongAnswers
            .filter { it != correct }
            .shuffled()
            .first()
    }

    private suspend fun endGame(finalScore: Int = _uiState.value.score) {
        timerJob?.cancel()

        // ✅ РЕАЛЬНОЕ сохранение результата в БД
        val userId = ServiceLocator.tokenManager.getUserData().firstOrNull()?.id ?: 1
        val result = GameResultEntity(
            userId = userId,
            score = finalScore,
            wordsCount = gameWords.size,
            timestamp = System.currentTimeMillis()
        )
        ServiceLocator.gameResultDao.insert(result)

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
