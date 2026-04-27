package com.example.t_learnappmobile.presentation.game

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.game.GameResultRequest
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.model.GameMode
import com.example.t_learnappmobile.domain.model.GameWord
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val MAX_WORDS = 10

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isNetworkAvailable.value = true
        }

        override fun onLost(network: Network) {
            _isNetworkAvailable.value = false
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isNetworkAvailable.value = hasInternet
        }
    }

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private var gameWords: List<GameWord> = emptyList()
    private var currentWordIndex = 0

    fun startGame(mode: GameMode) {
        viewModelScope.launch {
            _isLoading.value = true
            _uiState.value = GameState(isGameActive = false)
            loadWordsForGame()
        }
    }

    private suspend fun loadWordsForGame() {
        try {
            _isLoading.value = true
            val accessToken = ServiceLocator.tokenManager.getAccessToken().firstOrNull()
            if (accessToken == null) {
                Log.e("GameVM", "No access token")
                _uiState.value = GameState(isGameActive = false)
                return
            }

            val response = ServiceLocator.api.getAllWords("Bearer $accessToken")
            if (!response.isSuccessful) {
                Log.e("GameVM", "Failed to fetch words: ${response.code()}")
                _uiState.value = GameState(isGameActive = false)
                return
            }

            val wordResponses = response.body()?.words ?: emptyList()
            if (wordResponses.isEmpty()) {
                Log.w("GameVM", "No words available")
                _uiState.value = GameState(isGameActive = false, totalWords = 0)
                return
            }

            val allGameWords = wordResponses.map { w ->
                GameWord(
                    id = w.word.hashCode().toLong(),
                    english = w.word,
                    russian = w.translation
                )
            }

            gameWords = allGameWords.shuffled().take(MAX_WORDS)
            currentWordIndex = 0

            if (gameWords.isEmpty()) {
                _uiState.value = GameState(isGameActive = false, totalWords = 0)
                return
            }

            loadNextWord()
            _uiState.value = _uiState.value.copy(
                isGameActive = true,
                totalWords = gameWords.size,
                currentWordIndex = 0
            )
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
            wordsLeft = gameWords.size - currentWordIndex,
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
                return@launch
            }

            _uiState.value = state.copy(
                score = newScore,
                currentWordIndex = currentWordIndex
            )
            loadNextWord()
        }
    }

    private suspend fun endGame(finalScore: Int = _uiState.value.score) {
        try {
            val accessToken = ServiceLocator.tokenManager.getAccessToken().firstOrNull()
            if (accessToken != null) {
                ServiceLocator.gameApiService.saveGameResult(
                    "Bearer $accessToken",
                    GameResultRequest(
                        sessionScore = finalScore,
                        wordsCount = gameWords.size,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("GameVM", "Failed to save game result", e)
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