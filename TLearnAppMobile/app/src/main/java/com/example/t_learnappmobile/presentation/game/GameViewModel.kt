package com.example.t_learnappmobile.presentation.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.domain.usecase.game.LoadGameWordsUseCase
import com.example.t_learnappmobile.domain.usecase.game.SaveGameResultUseCase
import com.example.t_learnappmobile.domain.usecase.settings.SettingsUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    private val settingsUseCase: SettingsUseCase,
    private val wordRepository: WordRepository
) : ViewModel() {

    companion object {
        private const val TAG = "GameViewModel"
        private const val DELAY_BEFORE_NEXT_WORD_MS = 800L
        private const val POINTS_PER_CORRECT_ANSWER = 100
        private const val NUMBER_OF_OPTIONS = 2
        private const val LEARNED_STAGE = 8
        private const val MIN_WORDS_FOR_GAME = 10
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
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Пользователь не авторизован"
                        )
                    }
                    return@launch
                }

                val dictionaryId = settingsUseCase.getCurrentDictionaryId() ?: "finance"

                val userProgress = wordRepository.getUserProgress(userId, dictionaryId)

                Log.d(TAG, "User progress size: ${userProgress.size}")
                userProgress.forEach { progress ->
                    Log.d(TAG, "Progress - wordId: ${progress.wordId}, stage: ${progress.stage}")
                }

                val learnedOrInProgressWords = userProgress
                    .filter { it.stage > 0 && it.stage <= LEARNED_STAGE }
                    .map { it.wordId }
                    .toSet()

                Log.d(TAG, "Valid word ids (stage > 0): ${learnedOrInProgressWords.size}")

                if (learnedOrInProgressWords.size < MIN_WORDS_FOR_GAME) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Недостаточно слов для игры. Нужно минимум $MIN_WORDS_FOR_GAME изученных слов. Сейчас изучено: ${learnedOrInProgressWords.size}"
                        )
                    }
                    return@launch
                }

                val allGameWords = withTimeoutOrNull(5000L) {
                    loadGameWordsUseCase(dictionaryId, 100)
                } ?: emptyList()

                Log.d(TAG, "All game words loaded: ${allGameWords.size}")

                val wordsFromFirebase = wordRepository.getWordsFromFirebase(dictionaryId)
                Log.d(TAG, "Words from Firebase: ${wordsFromFirebase.size}")

                val wordIdToEnglish = wordsFromFirebase.associate { it.id to it.englishWord }

                gameWords = allGameWords
                    .filter { gameWord ->
                        val matchingWordId = wordIdToEnglish.entries.find {
                            it.value.equals(gameWord.english, ignoreCase = true)
                        }?.key

                        val isValid = matchingWordId != null && learnedOrInProgressWords.contains(matchingWordId)
                        if (!isValid) {
                            Log.d(TAG, "Filtered out word: ${gameWord.english} (no match in progress)")
                        } else {
                            Log.d(TAG, "Keeping word: ${gameWord.english} (matched with id: $matchingWordId)")
                        }
                        isValid
                    }
                    .shuffled()
                    .take(10)

                Log.d(TAG, "Final game words count: ${gameWords.size}")

                if (gameWords.size < MIN_WORDS_FOR_GAME) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Недостаточно подходящих слов для игры. Нужно минимум $MIN_WORDS_FOR_GAME слов. Найдено: ${gameWords.size}"
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
                Log.e(TAG, "Error starting game", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Не удалось загрузить игру. Проверьте интернет-соединение."
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
            .map { it.russian }
            .distinct()
            .shuffled()
            .take(NUMBER_OF_OPTIONS - 1)

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

        val finalScore = _uiState.value.score
        val totalWords = gameWords.size

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withTimeoutOrNull(5000L) {
                    saveGameResultUseCase(finalScore, totalWords)
                    Log.d(TAG, "Game result saved: score=$finalScore")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save game result", e)
            }
        }

        _uiState.update {
            it.copy(
                isGameActive = false,
                showResults = true,
                currentWord = null
            )
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