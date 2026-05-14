// presentation/game/GameViewModel.kt
package com.example.t_learnappmobile.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.usecase.game.LoadGameWordsUseCase
import com.example.t_learnappmobile.domain.usecase.game.SaveGameResultUseCase
import com.example.t_learnappmobile.domain.usecase.settings.SettingsUseCase
import kotlinx.coroutines.delay
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

class GameViewModel(
    private val loadGameWordsUseCase: LoadGameWordsUseCase,
    private val saveGameResultUseCase: SaveGameResultUseCase,
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameWords: List<GameWord> = emptyList()
    private var currentWordIndex = 0
    private var isAnswerInProgress = false // Добавляем флаг для предотвращения двойных нажатий

    fun startGame() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val dictionaryId = settingsUseCase.getCurrentDictionaryId() ?: "finance"
                gameWords = loadGameWordsUseCase(dictionaryId, 10)

                if (gameWords.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No words available for the game. Study some words first!"
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load game"
                )
            }
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
            .take(1) // Минимум 2 опции: правильный + 1 неправильный
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

    // presentation/game/GameViewModel.kt
// Замените метод selectAnswer:

    fun selectAnswer(selectedIndex: Int) {
        if (isAnswerInProgress) return

        val state = _uiState.value
        if (!state.isGameActive || state.currentWord == null) return

        isAnswerInProgress = true

        val isCorrect = selectedIndex == state.correctOptionIndex
        val points = if (isCorrect) 100 else 0
        val newScore = state.score + points

        _uiState.value = state.copy(
            score = newScore,
            lastAnswerCorrect = isCorrect
        )

        viewModelScope.launch {
            try {
                delay(800)
                currentWordIndex++

                if (currentWordIndex >= gameWords.size) {
                    endGame()
                } else {
                    loadNextWord()
                }
            } finally {
                isAnswerInProgress = false
            }
        }
    }

    private fun endGame() {
        viewModelScope.launch {
            val finalScore = _uiState.value.score

            // Сохраняем результат только один раз
            saveGameResultUseCase(finalScore, gameWords.size)

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