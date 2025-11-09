package com.example.t_learnappmobile.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.WordRepositoryImpl
import com.example.t_learnappmobile.model.VocabularyStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CardStats(
    val newWordsCount: Int = 0,
    val rotationWordsCount: Int = 0,
    val learnedWordsCount: Int = 0
)

class WordViewModel : ViewModel() {
    private val repository = WordRepositoryImpl()

    val currentWord = repository.getCurrentCardFlow().asLiveData()

    private val _isTranslationHidden = MutableLiveData(true)
    val isTranslationHidden: LiveData<Boolean> = _isTranslationHidden

    private val _cardStats = MutableLiveData<CardStats>()
    val cardStats: LiveData<CardStats> = _cardStats

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var timerJob: Job? = null

    init {
        loadInitialBatch()
        startPeriodicCheck()
    }

    private fun loadInitialBatch() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.fetchWordBatch(vocabularyId = 1, batchSize = 10)
                repository.triggerUpdate()
                updateStats()
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    private fun loadNextBatch() {
        viewModelScope.launch {
            try {
                repository.moveToNextCard()
                if (repository.wordsStorageSize < 2) {
                    repository.fetchWordBatch(1, 10)
                    repository.moveToNextCard()
                }
                updateStats()
            } catch (e: Exception) {
                _error.value = "Ошибка"
            }
        }
    }



    private fun startPeriodicCheck() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                repository.triggerUpdate()
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

    fun onKnowCard() {
        val card = currentWord.value
        if (card == null) {
            _error.value = "Карточка не загружена"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = repository.sendRotationAction(card.id, "know")
                _isLoading.value = false
                if (success) {
                    loadNextBatch()
                } else {
                    _error.value = "Ошибка отправки действия (mock fallback)"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun onDontKnowCard() {
        val card = currentWord.value
        if (card == null) {
            _error.value = "Карточка не загружена"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val success = repository.sendRotationAction(card.id, "dont_know")
                _isLoading.value = false
                if (success) {
                    loadNextBatch()
                } else {
                    _error.value = "Ошибка отправки действия (mock fallback)"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun toggleTranslation() {
        _isTranslationHidden.value = !(_isTranslationHidden.value ?: true)
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
