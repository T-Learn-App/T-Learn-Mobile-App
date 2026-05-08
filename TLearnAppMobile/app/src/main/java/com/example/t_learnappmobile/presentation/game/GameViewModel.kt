package com.example.t_learnappmobile.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.firebase.FirebaseGameRepository
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.model.GameWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameUiState(
    val currentWord: GameWord? = null,
    val score: Int = 0,
    val currentWordIndex: Int = 0,
    val totalWords: Int = 10,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val isGameActive: Boolean = false,
    val showResults: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastAnswerCorrect: Boolean? = null
)

class GameViewModel : ViewModel() {
    private val gameRepository = FirebaseGameRepository()
    private val settingsManager = ServiceLocator.appContext?.let {
        com.example.t_learnappmobile.data.settings.SettingsManager(it)
    }

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameWords: List<GameWord> = emptyList()
    private var currentWordIndex = 0

    fun startGame() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val dictionaryId = settingsManager?.getCurrentCategoryId() ?: "finance"
            gameWords = gameRepository.loadGameWords(dictionaryId, 10)

            if (gameWords.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Нет слов для игры. Изучите новые слова!"
                )
                return@launch
            }

            currentWordIndex = 0
            loadNextWord()
            _uiState.value = _uiState.value.copy(
                isGameActive = true,
                totalWords = gameWords.size,
                isLoading = false,
                score = 0
            )
        }
    }

    private fun loadNextWord() {
        if (currentWordIndex >= gameWords.size) {
            endGame()
            return
        }

        val word = gameWords[currentWordIndex]

        val otherAnswers = gameWords
            .filter { it.id != word.id }
            .shuffled()
            .take(1)
            .map { it.russian }

        val options = (listOf(word.russian) + otherAnswers).shuffled()
        val correctIndex = options.indexOf(word.russian)

        _uiState.value = _uiState.value.copy(
            currentWord = word,
            options = options,
            correctOptionIndex = correctIndex,
            currentWordIndex = currentWordIndex,
            lastAnswerCorrect = null
        )
    }

    fun selectAnswer(selectedIndex: Int) {
        val state = _uiState.value
        if (!state.isGameActive || state.currentWord == null) return

        val isCorrect = selectedIndex == state.correctOptionIndex
        val points = if (isCorrect) 100 else 0
        val newScore = state.score + points

        currentWordIndex++

        _uiState.value = state.copy(
            score = newScore,
            lastAnswerCorrect = isCorrect
        )

        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            if (currentWordIndex >= gameWords.size) {
                endGame()
            } else {
                loadNextWord()
            }
        }
    }

    private fun endGame() {
        viewModelScope.launch {
            val finalScore = _uiState.value.score

            // ✅ Сохраняем результат, но НЕ ждем его завершения
            gameRepository.saveGameResult(finalScore, gameWords.size)

            // ✅ Сразу показываем результаты, не дожидаясь сохранения
            _uiState.value = _uiState.value.copy(
                isGameActive = false,
                showResults = true,
                currentWord = null
            )
        }
    }

    fun closeResults() {
        _uiState.value = GameUiState()
    }
}