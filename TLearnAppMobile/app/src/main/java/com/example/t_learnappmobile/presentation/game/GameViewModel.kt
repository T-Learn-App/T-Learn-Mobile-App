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
import kotlinx.coroutines.flow.update
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

    companion object {
        private const val DELAY_BEFORE_NEXT_WORD_MS = 800L
        private const val POINTS_PER_CORRECT_ANSWER = 100
        private const val NUMBER_OF_OPTIONS = 2
    }

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameWords: List<GameWord> = emptyList()
    private var isAnswerProcessing = false
    private var isGameEnded = false

    fun startGame() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isGameActive = false) }

            try {
                val dictionaryId = settingsUseCase.getCurrentDictionaryId() ?: "finance"
                gameWords = loadGameWordsUseCase(dictionaryId, 10)

                if (gameWords.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Нет слов для игры. Изучите несколько слов сначала!"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isGameActive = true,
                        totalWords = gameWords.size,
                        isLoading = false,
                        score = 0,
                        currentWordIndex = 0
                    )
                }

                loadCurrentWord()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Не удалось загрузить игру"
                    )
                }
            }
        }
    }

    private fun loadCurrentWord() {
        val state = _uiState.value

        if (state.currentWordIndex >= gameWords.size) {
            endGame()
            return
        }

        val word = gameWords[state.currentWordIndex]

        val otherAnswers = gameWords
            .filter { it.id != word.id }
            .shuffled()
            .take(NUMBER_OF_OPTIONS - 1)
            .map { it.russian }


        val finalOtherAnswers = if (otherAnswers.isEmpty()) {
            listOf("???")
        } else {
            otherAnswers
        }

        val options = (listOf(word.russian) + finalOtherAnswers).shuffled()
        val correctIndex = options.indexOf(word.russian)

        _uiState.update {
            it.copy(
                currentWord = word,
                options = options,
                correctOptionIndex = correctIndex,
                lastAnswerCorrect = null
            )
        }
    }

    fun selectAnswer(selectedIndex: Int) {
        if (isAnswerProcessing) return

        val state = _uiState.value
        if (!state.isGameActive || state.currentWord == null || isGameEnded) return

        isAnswerProcessing = true

        val isCorrect = selectedIndex == state.correctOptionIndex
        val pointsEarned = if (isCorrect) POINTS_PER_CORRECT_ANSWER else 0
        val newScore = state.score + pointsEarned

        _uiState.update {
            it.copy(
                score = newScore,
                lastAnswerCorrect = isCorrect
            )
        }

        viewModelScope.launch {
            try {
                delay(DELAY_BEFORE_NEXT_WORD_MS)

                if (!isGameEnded) {
                    val currentIndex = _uiState.value.currentWordIndex
                    val nextIndex = currentIndex + 1

                    if (nextIndex >= gameWords.size) {
                        endGame()
                    } else {
                        _uiState.update { it.copy(currentWordIndex = nextIndex) }
                        loadCurrentWord()
                    }
                }
            } finally {
                isAnswerProcessing = false
            }
        }
    }

    private fun endGame() {
        if (isGameEnded) return

        isGameEnded = true
        isAnswerProcessing = false

        viewModelScope.launch {
            val finalScore = _uiState.value.score

            try {
                saveGameResultUseCase(finalScore, gameWords.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            _uiState.update {
                it.copy(
                    isGameActive = false,
                    showResults = true,
                    currentWord = null
                )
            }
        }
    }

    fun closeResults() {
        isGameEnded = false
        isAnswerProcessing = false
        _uiState.update { GameUiState() }
    }

    override fun onCleared() {
        super.onCleared()
        isGameEnded = true
        isAnswerProcessing = false
    }
}