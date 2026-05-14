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
import kotlinx.coroutines.delay
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
    val showDictionarySelection: Boolean = true  // По умолчанию показываем выбор
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

    private var wordQueue = mutableListOf<Word>()
    private var currentWordIndex = 0
    private var isProcessing = false

    init {
        Log.d(TAG, "ViewModel created")
        loadDictionaries()
    }

    private fun loadDictionaries() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== loadDictionaries START ===")

                // Загружаем словари
                val dicts = try {
                    getDictionariesUseCase()
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading dicts", e)
                    emptyList()
                }

                val displayDicts = dicts.ifEmpty {
                    Log.d(TAG, "Using default dictionaries")
                    listOf(
                        Dictionary("finance", "Финансы", 1),
                        Dictionary("conversational", "Разговорные слова", 2),
                        Dictionary("technology", "Технологии", 3),
                        Dictionary("slang", "Сленг", 4)
                    )
                }

                _uiState.value = _uiState.value.copy(
                    dictionaries = displayDicts,
                    isLoading = false,
                    showDictionarySelection = true  // Всегда показываем выбор словаря
                )

                Log.d(TAG, "=== loadDictionaries END - showing selection ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadDictionaries", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showDictionarySelection = true,
                    dictionaries = listOf(
                        Dictionary("finance", "Финансы", 1),
                        Dictionary("conversational", "Разговорные слова", 2),
                        Dictionary("technology", "Технологии", 3),
                        Dictionary("slang", "Сленг", 4)
                    )
                )
            }
        }
    }

    fun selectDictionary(dictionaryId: String) {
        Log.d(TAG, "=== selectDictionary: $dictionaryId ===")
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                Log.e(TAG, "User not authenticated, waiting...")
                // Ждем авторизацию
                var attempts = 0
                var uid: String? = null
                while (attempts < 30) {
                    uid = authRepository.getCurrentUserId()
                    if (uid != null) break
                    delay(200)
                    attempts++
                }
                if (uid == null) {
                    Log.e(TAG, "Still not authenticated after waiting")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showDictionarySelection = true
                    )
                    return@launch
                }
                selectDictionaryInternal(uid, dictionaryId)
            } else {
                selectDictionaryInternal(userId, dictionaryId)
            }
        }
    }

    private fun selectDictionaryInternal(userId: String, dictionaryId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Loading words for dict: $dictionaryId, userId: $userId")

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                showDictionarySelection = false,
                currentWord = null
            )

            val dict = _uiState.value.dictionaries.find { it.id == dictionaryId }
            if (dict != null) {
                _uiState.value = _uiState.value.copy(currentDictionary = dict)
                settingsUseCase.setCurrentDictionary(dictionaryId, dict.name)
                Log.d(TAG, "Saved dictionary: ${dict.name}")
            }

            when (val result = loadWordsUseCase(userId, dictionaryId)) {
                is LoadWordsResult.HasWords -> {
                    Log.d(TAG, "Loaded ${result.words.size} words")
                    wordQueue.clear()
                    wordQueue.addAll(result.words)
                    currentWordIndex = 0
                    showNextWord()
                    syncManager.syncPendingChanges()
                }
                is LoadWordsResult.Empty -> {
                    Log.d(TAG, "No words available")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentWord = null
                    )
                }
                is LoadWordsResult.Error -> {
                    Log.e(TAG, "Error: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentWord = null
                    )
                }
            }
        }
    }

    private fun showNextWord() {
        if (currentWordIndex >= wordQueue.size) {
            Log.d(TAG, "No more words")
            _uiState.value = _uiState.value.copy(isLoading = false, currentWord = null)
            return
        }

        val word = wordQueue[currentWordIndex]
        val direction = if (word.isNew) {
            TranslationDirection.EN_TO_RU
        } else {
            if (Random.nextBoolean()) TranslationDirection.EN_TO_RU else TranslationDirection.RU_TO_EN
        }

        Log.d(TAG, "Showing word: ${word.englishWord}")
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

    fun onKnowCard() { processAnswer(known = true) }
    fun onDontKnowCard() { processAnswer(known = false) }

    private fun processAnswer(known: Boolean) {
        if (isProcessing) return
        val currentWord = _uiState.value.currentWord ?: return
        isProcessing = true

        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId() ?: return@launch
                processAnswerUseCase(userId, currentWord.id, currentWord.dictionaryId, known)
                currentWordIndex++
                showNextWord()
                syncManager.syncPendingChanges()
            } finally {
                isProcessing = false
            }
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
        isProcessing = false
        _uiState.value = CardsUiState()
        loadDictionaries()
    }

    fun refreshStatistics() {}
}