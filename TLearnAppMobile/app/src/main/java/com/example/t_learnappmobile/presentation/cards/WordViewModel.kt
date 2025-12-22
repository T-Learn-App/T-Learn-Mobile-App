package com.example.t_learnappmobile.presentation.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.model.CardAction
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WordViewModel : ViewModel() {

    private val dictionaryManager = ServiceLocator.dictionaryManager

private val tokenManager = ServiceLocator.tokenManager
    private fun formatTodayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }
    private val repository: WordRepository = ServiceLocator.wordRepository

    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord

    private val _isTranslationHidden = MutableStateFlow(true)
    val isTranslationHidden: StateFlow<Boolean> = _isTranslationHidden

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadWords()
    }

    private fun loadWords() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = getUserId() ?: return@launch
                val vocabularyId = ServiceLocator.dictionaryManager.getCurrentVocabularyId(userId)
                repository.fetchWordBatch(userId, vocabularyId, 10)
                observeCurrentWord()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun observeCurrentWord() {
        viewModelScope.launch {
            repository.getCurrentCardFlow().collect { word ->
                _currentWord.value = word
                _isTranslationHidden.value = true
            }
        }
    }

    fun toggleTranslation() {
        _isTranslationHidden.value = !_isTranslationHidden.value
    }
    private suspend fun getUserId(): Int? {
        return tokenManager.getUserData().firstOrNull()?.id
    }

    suspend fun onKnowCard() {
        val word = _currentWord.value ?: return
        repository.sendRotationAction(word.id, CardAction.KNOW)

        val userId = getUserId() ?: return
        val today = formatTodayDate()
        val currentStats = dictionaryManager.getDailyStats(userId, today)

        val learnedInc = if (word.cardType == CardType.NEW) 1 else 0
        val updated = currentStats.copy(
            learnedWords = currentStats.learnedWords + learnedInc
        )
        dictionaryManager.saveDailyStats(userId, updated)

        repository.nextWord()
    }

    suspend fun onDontKnowCard() {
        val word = _currentWord.value ?: return
        repository.sendRotationAction(word.id, CardAction.DONT_KNOW)
        val userId = getUserId() ?: return
        val today = formatTodayDate()
        val currentStats = dictionaryManager.getDailyStats(userId, today)


        val newInc = if (word.cardType == CardType.NEW) 1 else 0
        val inProgressInc = if (word.cardType == CardType.ROTATION) 1 else 0

        val updated = currentStats.copy(
            newWords = currentStats.newWords + newInc,
            inProgressWords = currentStats.inProgressWords + inProgressInc
        )
        dictionaryManager.saveDailyStats(userId, updated)

        repository.nextWord()
    }

    fun refreshCurrentCard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = getUserId() ?: return@launch
                val vocabularyId = ServiceLocator.dictionaryManager.getCurrentVocabularyId(userId)
                repository.fetchWordBatch(userId, vocabularyId, 10)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
