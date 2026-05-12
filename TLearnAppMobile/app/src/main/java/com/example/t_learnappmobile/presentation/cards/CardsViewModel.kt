// presentation/cards/CardsViewModel.kt
package com.example.t_learnappmobile.presentation.cards

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.sync.SyncManager
import com.example.t_learnappmobile.domain.model.*
import com.example.t_learnappmobile.domain.repository.AuthRepository
import com.example.t_learnappmobile.domain.repository.LoadWordsResult
import com.example.t_learnappmobile.domain.usecase.settings.SettingsUseCase
import com.example.t_learnappmobile.domain.usecase.words.GetDictionariesUseCase
import com.example.t_learnappmobile.domain.usecase.words.LoadWordsUseCase
import com.example.t_learnappmobile.domain.usecase.words.ProcessAnswerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CardsUiState(
    val currentWord: Word? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val dictionaries: List<Dictionary> = emptyList(),
    val currentDictionary: Dictionary? = null,
    val isTranslationHidden: Boolean = true,
    val showDictionarySelection: Boolean = false
)

class CardsViewModel(
    private val authRepository: AuthRepository,
    private val loadWordsUseCase: LoadWordsUseCase,
    private val processAnswerUseCase: ProcessAnswerUseCase,
    private val getDictionariesUseCase: GetDictionariesUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val syncManager: SyncManager
) : ViewModel() {

    private val TAG = "CardsViewModel"
    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    // Состояние теперь в ViewModel
    private var wordQueue = mutableListOf<Word>()
    private var currentWordIndex = 0

    init {
        Log.d(TAG, "ViewModel created")
        loadDictionaries()
    }

    private fun loadDictionaries() {
        viewModelScope.launch {
            try {
                val dicts = getDictionariesUseCase()

                if (dicts.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Нет доступных словарей"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(dictionaries = dicts)

                val currentDictId = settingsUseCase.getCurrentDictionaryId()
                if (currentDictId != null) {
                    val currentDict = dicts.find { it.id == currentDictId }
                    if (currentDict != null) {
                        selectDictionary(currentDict.id)
                    } else {
                        selectDictionary(dicts.first().id)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showDictionarySelection = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading dictionaries", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun selectDictionary(dictionaryId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Пользователь не авторизован"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, showDictionarySelection = false)

            val dict = _uiState.value.dictionaries.find { it.id == dictionaryId }
            if (dict == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Словарь не найден")
                return@launch
            }

            _uiState.value = _uiState.value.copy(currentDictionary = dict)
            settingsUseCase.setCurrentDictionary(dictionaryId, dict.name)

            when (val result = loadWordsUseCase(userId, dictionaryId)) {
                is LoadWordsResult.HasWords -> {
                    wordQueue = result.words.toMutableList()
                    currentWordIndex = 0
                    showNextWord()

                    // Синхронизация теперь в ViewModel
                    syncManager.syncPendingChanges()
                }
                is LoadWordsResult.Empty -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, currentWord = null)
                }
                is LoadWordsResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    private fun showNextWord() {
        if (currentWordIndex >= wordQueue.size) {
            // Все слова закончились
            _uiState.value = _uiState.value.copy(isLoading = false, currentWord = null)
            return
        }

        val word = wordQueue[currentWordIndex]
        val direction = if (word.isNew) {
            TranslationDirection.EN_TO_RU
        } else {
            if (Random.nextBoolean()) TranslationDirection.EN_TO_RU else TranslationDirection.RU_TO_EN
        }

        _uiState.value = _uiState.value.copy(
            currentWord = word.copy(translationDirection = direction),
            isTranslationHidden = true,
            isLoading = false
        )
    }

    fun toggleTranslation() {
        _uiState.value = _uiState.value.copy(
            isTranslationHidden = !_uiState.value.isTranslationHidden
        )
    }

    fun onKnowCard() {
        processAnswer(known = true)
    }

    fun onDontKnowCard() {
        processAnswer(known = false)
    }

    private fun processAnswer(known: Boolean) {
        val currentWord = _uiState.value.currentWord ?: return

        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch

            // Обрабатываем ответ
            processAnswerUseCase(userId, currentWord.id, currentWord.dictionaryId, known)

            // Переходим к следующему слову
            currentWordIndex++
            showNextWord()

            // Синхронизируем изменения
            syncManager.syncPendingChanges()
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

    fun resetAndReload() {
        wordQueue.clear()
        currentWordIndex = 0
        _uiState.value = CardsUiState()
        loadDictionaries()
    }

    fun refreshStatistics() {
        // Можно обновить статистику при необходимости
    }
}