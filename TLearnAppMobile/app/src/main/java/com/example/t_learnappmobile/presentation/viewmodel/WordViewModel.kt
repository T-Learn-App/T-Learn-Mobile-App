package com.example.t_learnappmobile.presentation.viewmodel

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
    private fun loadInitialBatch(){

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

    }

    fun toggleTranslation() {
        _isTranslationHidden.value = !(_isTranslationHidden.value ?: true)
    }

    fun onKnowCard() { }
    fun onDontKnowCard() { }
    private fun loadNextBatch() { }

    private fun updateStats(stats: VocabularyStats?) { }
    override fun onCleared() { timerJob?.cancel(); super.onCleared() }

    }
