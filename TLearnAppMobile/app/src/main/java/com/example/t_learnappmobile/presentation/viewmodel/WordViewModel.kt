package com.example.t_learnappmobile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.repository.WordRepositoryImpl
import com.example.t_learnappmobile.data.repository.WordsStorage
import com.example.t_learnappmobile.domain.model.CardAction
import com.example.t_learnappmobile.model.Word

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CardStats(
    val newWordsCount: Int = 0,
    val rotationWordsCount: Int = 0,
    val learnedWordsCount: Int = 0
)

class WordViewModel : ViewModel() {
    private val repository = ServiceLocator.wordRepository

    val currentWord : StateFlow<Word?> = repository.getCurrentCardFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _isTranslationHidden = MutableStateFlow(true)
    val isTranslationHidden: MutableStateFlow<Boolean> = _isTranslationHidden


    private val _cardStats = MutableStateFlow(CardStats())
    val cardStats: MutableStateFlow<CardStats> = _cardStats


    private val _isLoading = MutableStateFlow(false)
    val isLoading: MutableStateFlow<Boolean> = _isLoading


    private val _error = MutableSharedFlow<String?>(replay = 0)
    val error: SharedFlow<String?> = _error

    private var timerJob: Job? = null

    init {
        loadInitialBatch()
        startPeriodicCheck()
    }
    private fun loadInitialBatch() {
        viewModelScope.launch {
            _isLoading.emit(true)
            try {
                repository.fetchWordBatch(vocabularyId = 1, batchSize = 10)
            } catch (e: Exception) {
                _error.emit("Ошибка загрузки")
            } finally {
                _isLoading.emit(false)
            }
        }
    }


    private fun startPeriodicCheck() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                updateStats()
            }
        }
    }

    private fun updateStats() {
        val newCount = repository.getNewWords().size
        val rotationCount = repository.getRotationWords().size
        val learnedCount = repository.getLearnedWords().size
        _cardStats.value = CardStats(newCount, rotationCount, learnedCount)
    }

    fun toggleTranslation() {
        _isTranslationHidden.value = !_isTranslationHidden.value
    }

    suspend fun onKnowCard() {
        val card = currentWord.value
        if (card == null) {
            _error.emit("Карточка не загружена")
            return
        }

            _isLoading.value = true
            _error.emit(null)
            try {
                val success = repository.sendRotationAction(card.id, CardAction.KNOW)
                _isLoading.value = false
                if (success) {
                    _isTranslationHidden.value = true
                    repository.nextWord()


                    updateStats()
                } else {
                    _error.emit("Ошибка отправки действия (mock fallback)")
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.emit("Ошибка: ${e.message}")
            }
    }

    suspend fun onDontKnowCard() {
        val card = currentWord.value
        if (card == null) {
            _error.emit("Карточка не загружена")
            return
        }

            _isLoading.value = true
            _error.emit(null)
            try {
                val success = repository.sendRotationAction(card.id, CardAction.DONT_KNOW)
                _isLoading.value = false
                if (success) {
                    _isTranslationHidden.value = true
                    repository.nextWord()
                    updateStats()
                } else {
                    _error.emit("Ошибка отправки действия (mock fallback)")
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.emit("Ошибка: ${e.message}")
            }

    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

}






