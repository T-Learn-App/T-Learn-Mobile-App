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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CardsUiState(
    val currentWord: Word? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val dictionaries: List<Dictionary> = emptyList(),
    val currentDictionary: Dictionary? = null,
    val isTranslationHidden: Boolean = true,
    val showDictionarySelection: Boolean = true,
    val isTransitioning: Boolean = false
)

class CardsViewModel(
    private val authRepository: AuthRepository,
    private val loadWordsUseCase: LoadWordsUseCase,
    private val processAnswerUseCase: ProcessAnswerUseCase,
    private val getDictionariesUseCase: GetDictionariesUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val syncManager: SyncManager
) : ViewModel() {

    companion object {
        private const val TAG = "CardsViewModel"
        private const val BACKGROUND_CHECK_INTERVAL_MS = 30_000L
        private const val MAX_AUTH_ATTEMPTS = 30
        private const val AUTH_RETRY_DELAY_MS = 200L
        private const val TRANSITION_DELAY_MS = 150L
    }

    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    private var wordQueue = mutableListOf<Word>()
    private var currentWordIndex = 0
    private var isProcessing = false
    private var backgroundCheckJob: Job? = null
    private var isBackgroundCheckRunning = false

    init {
        Log.d(TAG, "ViewModel created")
        loadDictionaries()
    }

    override fun onCleared() {
        super.onCleared()
        stopBackgroundCheck()
        Log.d(TAG, "ViewModel cleared")
    }



    fun startBackgroundCheck() {
        if (isBackgroundCheckRunning) {
            Log.d(TAG, "Background check already running")
            return
        }

        stopBackgroundCheck()
        isBackgroundCheckRunning = true

        backgroundCheckJob = viewModelScope.launch {
            Log.d(TAG, "Background check started (interval: ${BACKGROUND_CHECK_INTERVAL_MS}ms)")
            while (isActive && isBackgroundCheckRunning) {
                delay(BACKGROUND_CHECK_INTERVAL_MS)
                if (shouldPerformBackgroundCheck()) {
                    checkAndShowNextWordIfAvailable()
                }
            }
            Log.d(TAG, "Background check ended")
        }
    }

    fun stopBackgroundCheck() {
        isBackgroundCheckRunning = false
        backgroundCheckJob?.cancel()
        backgroundCheckJob = null
        Log.d(TAG, "Background check stopped")
    }

    fun manualCheck() {
        viewModelScope.launch {
            Log.d(TAG, "Manual check triggered")
            if (shouldPerformBackgroundCheck()) {
                checkAndShowNextWordIfAvailable()
            }
        }
    }

    fun selectDictionary(dictionaryId: String) {
        Log.d(TAG, "=== selectDictionary: $dictionaryId ===")
        viewModelScope.launch {
            val userId = getUserIdWithRetry()
            if (userId == null) {
                Log.e(TAG, "User not authenticated after retries")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showDictionarySelection = true,
                        error = "Ошибка аутентификации"
                    )
                }
                return@launch
            }
            selectDictionaryInternal(userId, dictionaryId)
        }
    }

    fun toggleTranslation() {
        _uiState.update {
            it.copy(isTranslationHidden = !it.isTranslationHidden)
        }
    }

    fun onKnowCard() = processAnswer(known = true)
    fun onDontKnowCard() = processAnswer(known = false)

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
        stopBackgroundCheck()
        wordQueue.clear()
        currentWordIndex = 0
        isProcessing = false
        _uiState.update { CardsUiState() }
        loadDictionaries()
    }



    private suspend fun getUserIdWithRetry(): String? {
        var userId = authRepository.getCurrentUserId()
        if (userId != null) return userId

        Log.d(TAG, "User not authenticated, waiting...")
        repeat(MAX_AUTH_ATTEMPTS) {
            delay(AUTH_RETRY_DELAY_MS)
            userId = authRepository.getCurrentUserId()
            if (userId != null) return userId
        }
        return null
    }

    private fun shouldPerformBackgroundCheck(): Boolean {
        val state = _uiState.value
        return state.currentWord == null &&
                !state.isLoading &&
                state.currentDictionary != null &&
                !isProcessing &&
                !state.isTransitioning
    }

    private fun loadDictionaries() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== loadDictionaries START ===")

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

                _uiState.update {
                    it.copy(
                        dictionaries = displayDicts,
                        isLoading = false,
                        showDictionarySelection = true
                    )
                }

                Log.d(TAG, "=== loadDictionaries END ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadDictionaries", e)
                _uiState.update {
                    it.copy(
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
    }

    private suspend fun selectDictionaryInternal(userId: String, dictionaryId: String) {
        Log.d(TAG, "Loading words for dict: $dictionaryId, userId: $userId")

        stopBackgroundCheck()

        _uiState.update {
            it.copy(
                isLoading = true,
                showDictionarySelection = false,
                currentWord = null,
                isTranslationHidden = true,
                isTransitioning = false
            )
        }

        val dict = _uiState.value.dictionaries.find { it.id == dictionaryId }
        if (dict != null) {
            _uiState.update { it.copy(currentDictionary = dict) }
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
                startBackgroundCheck()
            }
            is LoadWordsResult.Empty -> {
                Log.d(TAG, "No words available")
                _uiState.update { it.copy(isLoading = false, currentWord = null) }
                startBackgroundCheck()
            }
            is LoadWordsResult.Error -> {
                Log.e(TAG, "Error: ${result.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentWord = null,
                        error = result.message
                    )
                }
            }
        }
    }

    private suspend fun checkAndShowNextWordIfAvailable() {
        val state = _uiState.value

        if (state.currentWord != null) {
            Log.d(TAG, "Word is showing, skip check")
            return
        }

        if (state.isLoading) {
            Log.d(TAG, "Loading in progress, skip check")
            return
        }

        if (state.isTransitioning) {
            Log.d(TAG, "Transition in progress, skip check")
            return
        }

        val dictionary = state.currentDictionary
        if (dictionary == null) {
            Log.d(TAG, "No dictionary selected, skip check")
            return
        }

        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "User not authenticated, skip check")
            return
        }

        Log.d(TAG, "Checking for available words in dictionary: ${dictionary.id}")

        when (val result = loadWordsUseCase(userId, dictionary.id)) {
            is LoadWordsResult.HasWords -> {
                if (result.words.isNotEmpty()) {
                    Log.d(TAG, "Found ${result.words.size} words available for review!")
                    wordQueue.clear()
                    wordQueue.addAll(result.words)
                    currentWordIndex = 0
                    showNextWord()
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    Log.d(TAG, "No words available yet")
                }
            }
            is LoadWordsResult.Empty -> {
                Log.d(TAG, "Word queue is empty")
            }
            is LoadWordsResult.Error -> {
                Log.e(TAG, "Error checking words: ${result.message}")
            }
        }
    }


    private fun showNextWord() {
        if (currentWordIndex >= wordQueue.size) {
            Log.d(TAG, "No more words in queue")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentWord = null,
                    isTransitioning = false
                )
            }
            return
        }

        val word = wordQueue[currentWordIndex]
        val direction = if (word.isNew) {
            TranslationDirection.EN_TO_RU
        } else {
            if (Random.nextBoolean()) TranslationDirection.EN_TO_RU else TranslationDirection.RU_TO_EN
        }

        Log.d(TAG, "Showing word: ${word.englishWord}")

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTransitioning = true,
                    isTranslationHidden = true
                )
            }

            delay(TRANSITION_DELAY_MS)

            _uiState.update {
                it.copy(
                    currentWord = word.copy(translationDirection = direction),
                    isLoading = false,
                    isTransitioning = false
                )
            }
        }
    }

    private fun processAnswer(known: Boolean) {
        if (isProcessing) return

        val currentWord = _uiState.value.currentWord
        if (currentWord == null) {
            Log.w(TAG, "No current word to process")
            return
        }

        if (!_uiState.value.isTranslationHidden) {
            _uiState.update { it.copy(isTranslationHidden = true) }
        }

        isProcessing = true

        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    Log.e(TAG, "User not authenticated")
                    return@launch
                }

                processAnswerUseCase(userId, currentWord.id, currentWord.dictionaryId, known)
                currentWordIndex++

                if (currentWordIndex < wordQueue.size) {
                    showNextWord()
                } else {
                    _uiState.update {
                        it.copy(
                            currentWord = null,
                            isTransitioning = false,
                            isTranslationHidden = true
                        )
                    }
                    checkAndShowNextWordIfAvailable()
                }

                try {
                    syncManager.syncPendingChanges()
                } catch (e: Exception) {
                    Log.e(TAG, "Sync error after answer", e)
                }
            } finally {
                isProcessing = false
            }
        }
    }
}