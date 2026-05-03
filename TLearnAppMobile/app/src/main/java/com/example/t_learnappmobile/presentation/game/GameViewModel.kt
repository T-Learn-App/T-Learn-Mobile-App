package com.example.t_learnappmobile.presentation.game

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.firebase.FirebaseGameRepository
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.model.GameMode
import com.example.t_learnappmobile.domain.model.GameWord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val gameRepository = FirebaseGameRepository()
    private val MAX_WORDS = 10

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var gameWords: List<GameWord> = emptyList()
    private var currentWordIndex = 0
    private var currentDictionaryId = ""

    fun setDictionary(dictionaryId: String) {
        currentDictionaryId = dictionaryId
    }

    fun startGame(mode: GameMode) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = GameState(isGameActive = false)

            try {
                gameWords = gameRepository.loadGameWords(currentDictionaryId, MAX_WORDS)

                if (gameWords.isEmpty()) {
                    _uiState.value = GameState(isGameActive = false, totalWords = 0)
                    _isLoading.value = false
                    return@launch
                }

                currentWordIndex = 0
                loadNextWord()
                _uiState.value = _uiState.value.copy(
                    isGameActive = true,
                    totalWords = gameWords.size
                )
            } catch (e: Exception) {
                Log.e("GameViewModel", "Error starting game", e)
                _uiState.value = GameState(isGameActive = false, totalWords = 0)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadNextWord() {
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
            wordsLeft = gameWords.size - currentWordIndex
        )
    }

    fun selectAnswer(selectedIndex: Int) {
        val state = _uiState.value
        if (!state.isGameActive || state.currentWord == null) return

        val isCorrect = selectedIndex == state.correctOptionIndex
        val points = if (isCorrect) 100 else 0
        val newScore = state.score + points

        currentWordIndex++

        viewModelScope.launch {
            if (currentWordIndex >= gameWords.size) {
                endGame(newScore)
            } else {
                _uiState.value = state.copy(
                    score = newScore,
                    currentWordIndex = currentWordIndex
                )
                loadNextWord()
            }
        }
    }

    private suspend fun endGame(finalScore: Int = _uiState.value.score) {
        try {
            gameRepository.saveGameResult(finalScore, gameWords.size)
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error saving game result", e)
        }

        _uiState.value = _uiState.value.copy(
            isGameActive = false,
            showResults = true,
            score = finalScore,
            totalWords = gameWords.size
        )
    }

    fun closeResults() {
        _uiState.value = GameState()
    }
}