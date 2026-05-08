// Файл: presentation/cards/CardsViewModel.kt
package com.example.t_learnappmobile.presentation.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Dictionary
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CardsUiState(
    val currentWord: Word? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val dictionaries: List<Dictionary> = emptyList(),
    val currentDictionary: Dictionary? = null,
    val isTranslationHidden: Boolean = true
)

class CardsViewModel : ViewModel() {
    private val wordRepository = ServiceLocator.wordRepository
    private val settingsManager = ServiceLocator.appContext?.let {
        com.example.t_learnappmobile.data.settings.SettingsManager(it)
    }

    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    init {
        loadDictionaries()
        observeCurrentWord()
    }

    private fun observeCurrentWord() {
        viewModelScope.launch {
            wordRepository.getCurrentWordFlow().collect { word ->
                _uiState.value = _uiState.value.copy(
                    currentWord = word,
                    isLoading = false,
                    isTranslationHidden = true
                )
            }
        }
    }

    private fun loadDictionaries() {
        viewModelScope.launch {
            try {
                val dicts = wordRepository.getDictionaries()
                _uiState.value = _uiState.value.copy(dictionaries = dicts)
                if (dicts.isNotEmpty()) {
                    val currentDictId = settingsManager?.getCurrentCategoryId() ?: dicts.first().id
                    val currentDict = dicts.find { it.id == currentDictId } ?: dicts.first()
                    selectDictionary(currentDict.id)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun selectDictionary(dictionaryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val dict = _uiState.value.dictionaries.find { it.id == dictionaryId }
                _uiState.value = _uiState.value.copy(currentDictionary = dict)
                settingsManager?.setCurrentCategoryId(dictionaryId)
                settingsManager?.setCurrentDictionaryName(dict?.name ?: "")
                wordRepository.loadWords(dictionaryId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun retryLoad() {
        _uiState.value.currentDictionary?.let { selectDictionary(it.id) }
    }

    fun toggleTranslation() {
        _uiState.value = _uiState.value.copy(
            isTranslationHidden = !_uiState.value.isTranslationHidden
        )
    }

    fun onKnowCard() {
        _uiState.value.currentWord?.let { word ->
            wordRepository.answerWord(word.id, known = true)
        }
    }

    fun onDontKnowCard() {
        _uiState.value.currentWord?.let { word ->
            wordRepository.answerWord(word.id, known = false)
        }
    }

    fun getCardType(): CardType {
        val word = _uiState.value.currentWord ?: return CardType.NEW
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