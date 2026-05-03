package com.example.t_learnappmobile.presentation.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.model.GameMode
import com.example.t_learnappmobile.domain.model.GameWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val MAX_WORDS = 10
    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var gameWords: List<GameWord> = emptyList()
    private var currentWordIndex = 0

    fun startGame(mode: GameMode) {
        viewModelScope.launch {
            _isLoading.value = true
            loadWordsForGame()
        }
    }

    private suspend fun loadWordsForGame() {
        try {
            val token = ServiceLocator.firebaseAuthManager.getAccessToken()
            if (token == null) {
                _uiState.value = GameState(isGameActive = false)
                return
            }

            val response = ServiceLocator.wordApi.getAllWords("Bearer $token")
            if (!response.isSuccessful) {
                _uiState.value = GameState(isGameActive = false)
                return
            }

            val wordResponses = response.body()?.words ?: emptyList()
            if (wordResponses.isEmpty()) {
                _uiState.value = GameState(isGameActive = false, totalWords = 0)
                return
            }

            gameWords = wordResponses.shuffled().take(MAX_WORDS).map { w ->
                GameWord(id = w.word.hashCode().toLong(), english = w.word, russian = w.translation)
            }
            currentWordIndex = 0

            if (gameWords.isEmpty()) {
                _uiState.value = GameState(isGameActive = false, totalWords = 0)
                return
            }

            loadNextWord()
            _uiState.value = _uiState.value.copy(isGameActive = true, totalWords = gameWords.size)
        } catch (e: Exception) {
            Log.e("GameVM", "Error loading words", e)
            _uiState.value = GameState(isGameActive = false)
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun loadNextWord() {
        if (currentWordIndex >= gameWords.size) {
            endGame()
            return
        }

        val word = gameWords[currentWordIndex]
        val otherAnswers = gameWords.filter { it.id != word.id }.shuffled().take(1).map { it.russian }
        val options = (listOf(word.russian) + otherAnswers).shuffled()
        val correctIndex = options.indexOf(word.russian)

        _uiState.value = _uiState.value.copy(
            currentWord = word,
            options = options,
            correctOptionIndex = correctIndex,
            currentWordIndex = currentWordIndex
        )
    }

    fun selectAnswer(selectedIndex: Int) {
        val state = _uiState.value
        if (!state.isGameActive || state.currentWord == null) return

        val isCorrect = selectedIndex == state.correctOptionIndex
        val points = if (isCorrect) 100 else 0
        val newScore = state.score + points

        viewModelScope.launch {
            currentWordIndex++
            if (currentWordIndex >= gameWords.size) {
                endGame(newScore)
            } else {
                _uiState.value = state.copy(score = newScore, currentWordIndex = currentWordIndex)
                loadNextWord()
            }
        }
    }

    private suspend fun endGame(finalScore: Int = _uiState.value.score) {
        try {
            ServiceLocator.userRepository.updateGameScore(finalScore)
        } catch (e: Exception) {
            Log.e("GameVM", "Failed to save game result", e)
        }
        _uiState.value = _uiState.value.copy(isGameActive = false, showResults = true, score = finalScore, totalWords = gameWords.size)
    }

    fun closeResults() { _uiState.value = GameState() }
}