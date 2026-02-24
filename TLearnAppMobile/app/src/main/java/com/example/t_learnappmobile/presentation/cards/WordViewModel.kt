package com.example.t_learnappmobile.presentation.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.repository.ServiceLocator.storage
import com.example.t_learnappmobile.domain.model.CardAction
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WordViewModel : ViewModel() {

    private val dictionaryManager = ServiceLocator.dictionaryManager
    private val tokenManager = ServiceLocator.tokenManager
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
        observeCurrentCard()
        observeDictionaryChanges()
        loadInitialWords()
    }


    private fun observeCurrentCard() {
        viewModelScope.launch {
            repository.getCurrentCardFlow().collect { word ->
                _currentWord.value = word
                _isTranslationHidden.value = true
            }
        }
    }


    private fun observeDictionaryChanges() {
        viewModelScope.launch {
            dictionaryManager.currentVocabularyIdFlow
                .filterNotNull()
                .distinctUntilChanged()
                .collect { catId ->
                    loadWordsForCategory(catId.toLong())
                }
        }
    }


    private fun loadInitialWords() {
        viewModelScope.launch {
            val userId = getUserId()
            if (userId != null) {
                val initialCategory = dictionaryManager.getCurrentVocabularyId(userId).toLong()
                loadWordsForCategory(initialCategory)
            }
        }
    }


    fun fetchWords() {
        viewModelScope.launch {
            val userId = getUserId() ?: return@launch
            val categoryId = dictionaryManager.getCurrentVocabularyId(userId).toLong()
            loadWordsForCategory(categoryId)
        }
    }

    private suspend fun loadWordsForCategory(categoryId: Long) {
        _isLoading.value = true
        _currentWord.value = null
        _error.value = null

        try {
            val words = repository.fetchWords(categoryId)
            storage.updateWords(words)

            repository.nextWord()
        } catch (e: Exception) {
            _error.value = e.message ?: "Не удалось загрузить слова"
        } finally {
            _isLoading.value = false
        }
    }

    private fun formatTodayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private suspend fun getUserId(): Int? {
        return ServiceLocator.tokenManager.getUserId()?.toInt() ?: 0
    }

    fun toggleTranslation() {
        _isTranslationHidden.value = !_isTranslationHidden.value
    }

    suspend fun onKnowCard() {
        val word = _currentWord.value ?: return
        repository.completeWord(word.id)

        val userId = getUserId() ?: return
        val today = formatTodayDate()
        val current = dictionaryManager.getDailyStats(userId, today)
        val updated = current.copy(learnedWords = current.learnedWords + 1)
        dictionaryManager.saveDailyStats(userId, updated)

        repository.nextWord()
    }

    suspend fun onDontKnowCard() {
        val word = _currentWord.value ?: return
        repository.completeWord(word.id)

        val userId = getUserId() ?: return
        val today = formatTodayDate()
        val current = dictionaryManager.getDailyStats(userId, today)
        val updated = current.copy(
            newWords = current.newWords + if (word.cardType == CardType.NEW) 1 else 0,
            inProgressWords = current.inProgressWords + if (word.cardType == CardType.ROTATION) 1 else 0
        )
        dictionaryManager.saveDailyStats(userId, updated)

        repository.nextWord()
    }
}
