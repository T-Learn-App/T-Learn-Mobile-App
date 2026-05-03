package com.example.t_learnappmobile.presentation.cards

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.firebase.FirebaseWordRepository
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Dictionary
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WordViewModel(
    private val wordRepository: WordRepository
) : ViewModel() {

    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _dictionaries = MutableStateFlow<List<Dictionary>>(emptyList())
    val dictionaries: StateFlow<List<Dictionary>> = _dictionaries.asStateFlow()

    private val _currentDictionary = MutableStateFlow<Dictionary?>(null)
    val currentDictionary: StateFlow<Dictionary?> = _currentDictionary.asStateFlow()

    private val _isTranslationHidden = MutableStateFlow(true)
    val isTranslationHidden: StateFlow<Boolean> = _isTranslationHidden.asStateFlow()

    init {
        loadDictionaries()
        startListeningToWords()
    }

    private fun startListeningToWords() {
        viewModelScope.launch {
            wordRepository.getCurrentWordFlow().collect { word ->
                _currentWord.value = word
                _isTranslationHidden.value = true
                _isLoading.value = false
            }
        }
    }

    private fun loadDictionaries() {
        viewModelScope.launch {
            try {
                Log.d("WordViewModel", "loadDictionaries START")
                _isLoading.value = true
                val dicts = wordRepository.getDictionaries()
                Log.d("WordViewModel", "Loaded ${dicts.size} dictionaries")
                _dictionaries.value = dicts
                if (dicts.isNotEmpty()) {
                    Log.d("WordViewModel", "First dictionary: ${dicts.first().name}")
                    selectDictionary(dicts.first().id)
                }
                _error.value = null
            } catch (e: Exception) {
                Log.e("WordViewModel", "Error loading dictionaries", e)
                _error.value = e.message ?: "Ошибка загрузки словарей"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDictionary(dictionaryId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _currentDictionary.value = _dictionaries.value.find { it.id == dictionaryId }
                wordRepository.loadWords(dictionaryId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Ошибка загрузки слов"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryLoad() {
        _error.value = null
        _currentDictionary.value?.let {
            selectDictionary(it.id)
        } ?: loadDictionaries()
    }

    fun toggleTranslation() {
        _isTranslationHidden.value = !_isTranslationHidden.value
    }

    fun onKnowCard() {
        _currentWord.value?.let { word ->
            wordRepository.answerWord(word.id, known = true)
        }
    }

    fun onDontKnowCard() {
        _currentWord.value?.let { word ->
            wordRepository.answerWord(word.id, known = false)
        }
    }

    fun getCardType(): CardType {
        val word = _currentWord.value ?: return CardType.NEW
        return if (word.isNew) CardType.NEW else CardType.ROTATION
    }

    fun getButtonTexts(): Pair<String, String> {
        return if (getCardType() == CardType.NEW) {
            "Я знаю это слово" to "Я не знаю это слово"
        } else {
            "Я запомнил" to "Я не запомнил"
        }
    }
}