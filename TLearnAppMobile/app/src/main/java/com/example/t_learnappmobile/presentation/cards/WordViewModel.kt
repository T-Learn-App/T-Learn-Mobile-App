package com.example.t_learnappmobile.presentation.cards

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Dictionary
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WordViewModel(
    private val wordRepository: WordRepository,
    private val prefs: SharedPreferences
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

    private var autoRefreshJob: Job? = null

    companion object {
        private const val KEY_CURRENT_CATEGORY = "current_category_id"
        private const val DEFAULT_CATEGORY_ID = "finance"
        private const val AUTO_REFRESH_INTERVAL = 60_000L // Проверяем каждую минуту
    }

    init {
        loadDictionaries()
        startListeningToWords()
        startAutoRefresh()
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

    /**
     * Автоматически проверяет наличие новых слов для показа
     */
    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL)
                if (_currentWord.value == null && _currentDictionary.value != null) {
                    Log.d("WordViewModel", "Auto-refreshing words...")
                    wordRepository.loadWords(_currentDictionary.value!!.id)
                }
            }
        }
    }

    private fun loadDictionaries() {
        viewModelScope.launch {
            try {
                Log.d("WordViewModel", "loadDictionaries START")
                _isLoading.value = true
                val dicts = wordRepository.getDictionaries()
                Log.d("WordViewModel", "Loaded ${dicts.size} dictionaries: ${dicts.map { it.name }}")
                _dictionaries.value = dicts

                if (dicts.isNotEmpty()) {
                    Log.d("WordViewModel", "First dictionary: ${dicts.first().name}")
                    selectDictionary(getCurrentCategoryId())
                } else {
                    Log.e("WordViewModel", "No dictionaries loaded!")
                    _error.value = "Нет доступных словарей"
                }
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
                Log.d("WordViewModel", "selectDictionary: $dictionaryId")
                _isLoading.value = true
                _error.value = null

                val selectedDict = _dictionaries.value.find { it.id == dictionaryId }
                _currentDictionary.value = selectedDict
                Log.d("WordViewModel", "Selected dictionary: ${selectedDict?.name}")

                saveCurrentCategoryId(dictionaryId)
                wordRepository.loadWords(dictionaryId)
            } catch (e: Exception) {
                Log.e("WordViewModel", "Error selecting dictionary", e)
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
        val word = _currentWord.value ?: return
        Log.d("WordViewModel", "onKnowCard: ${word.englishWord}, userWordDocId: ${word.userWordDocId}")

        wordRepository.answerWord(word.userWordDocId, known = true)
        wordRepository.markCurrentWordAsShown()
    }

    fun onDontKnowCard() {
        val word = _currentWord.value ?: return
        Log.d("WordViewModel", "onDontKnowCard: ${word.englishWord}, userWordDocId: ${word.userWordDocId}")

        wordRepository.answerWord(word.userWordDocId, known = false)
        wordRepository.markCurrentWordAsShown()
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

    private fun getCurrentCategoryId(): String {
        return prefs.getString(KEY_CURRENT_CATEGORY, DEFAULT_CATEGORY_ID) ?: DEFAULT_CATEGORY_ID
    }

    private fun saveCurrentCategoryId(categoryId: String) {
        prefs.edit().putString(KEY_CURRENT_CATEGORY, categoryId).apply()
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}