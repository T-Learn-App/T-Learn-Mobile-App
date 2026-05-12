package com.example.t_learnappmobile.presentation.cards

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.repository.LoadWordsResult
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Dictionary
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val isTranslationHidden: Boolean = true,
    val showDictionarySelection: Boolean = false
)

class CardsViewModel : ViewModel() {
    private val TAG = "CardsViewModel"
    private val settingsManager = ServiceLocator.appContext?.let {
        com.example.t_learnappmobile.data.settings.SettingsManager(it)
    }

    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        Log.d(TAG, "ViewModel created")
        startObservingCurrentWord()
        loadDictionaries()
    }

    private fun startObservingCurrentWord() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            Log.d(TAG, "👀 Starting to observe word flow from ${ServiceLocator.wordRepository.hashCode()}")

            val timeoutJob = launch {
                delay(5_000)
                if (_uiState.value.isLoading) {
                    Log.d(TAG, "⚠️ Timeout — no word received in 5 seconds")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentWord = null
                    )
                }
            }

            ServiceLocator.wordRepository.getCurrentWordFlow().collect { word ->
                timeoutJob.cancel()
                Log.d(TAG, "📖 Flow emitted: word=${word?.englishWord ?: "null"}, stage=${word?.stage}")
                _uiState.value = _uiState.value.copy(
                    currentWord = word,
                    isTranslationHidden = true,
                    isLoading = false
                )
            }
        }
    }

    private fun loadDictionaries() {
        viewModelScope.launch {
            Log.d(TAG, "loadDictionaries START")
            try {
                val dicts = ServiceLocator.wordRepository.getDictionaries()
                Log.d(TAG, "Loaded ${dicts.size} dictionaries")

                if (dicts.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Нет доступных словарей"
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(dictionaries = dicts)

                val hasSelected = settingsManager?.hasSelectedDictionary() ?: false
                Log.d(TAG, "hasSelectedDictionary = $hasSelected")

                if (hasSelected) {
                    val savedDictId = settingsManager?.getCurrentCategoryId()
                    val currentDict = dicts.find { it.id == savedDictId } ?: dicts.first()
                    selectDictionary(currentDict.id)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showDictionarySelection = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadDictionaries ERROR", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }



    fun selectDictionary(dictionaryId: String) {
        viewModelScope.launch {
            Log.d(TAG, "selectDictionary: $dictionaryId")
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                showDictionarySelection = false
            )

            try {
                val dict = _uiState.value.dictionaries.find { it.id == dictionaryId }
                if (dict == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Словарь не найден",
                        showDictionarySelection = true
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(currentDictionary = dict)

                settingsManager?.setCurrentCategoryId(dictionaryId)
                settingsManager?.setCurrentDictionaryName(dict.name)


                startObservingCurrentWord()


                val result = ServiceLocator.wordRepository.loadWords(dictionaryId)
                Log.d(TAG, "loadWords result: $result")

                when (result) {
                    is LoadWordsResult.HasWords -> {

                        Log.d(TAG, "Waiting for flow...")
                    }
                    is LoadWordsResult.Empty -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            currentWord = null
                        )
                    }
                    is LoadWordsResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message,
                            showDictionarySelection = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "selectDictionary ERROR", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message,
                    showDictionarySelection = true
                )
            }
        }
    }

    fun resetAndReload() {
        Log.d(TAG, "resetAndReload")
        viewModelScope.launch {
            _uiState.value = CardsUiState()
            startObservingCurrentWord()
            loadDictionaries()
        }
    }



    fun toggleTranslation() {
        _uiState.value = _uiState.value.copy(
            isTranslationHidden = !_uiState.value.isTranslationHidden
        )
    }
    fun refreshStatistics() {
        viewModelScope.launch {
            try {
                val userId = ServiceLocator.firebaseAuthManager.getUserId() ?: return@launch
                val dictionaryId = _uiState.value.currentDictionary?.id ?: return@launch

                val context = ServiceLocator.appContext
                val database = com.example.t_learnappmobile.data.local.AppDatabase.getInstance(context)
                val userWords = database.wordDao().getUserWords(userId, dictionaryId)

                var newWords = 0
                var inProgress = 0
                var learned = 0

                for (word in userWords) {
                    when {
                        word.stage == 0 -> newWords++
                        word.stage in 1..7 -> inProgress++
                        word.stage >= 8 -> learned++
                    }
                }

                Log.d(TAG, "📊 Statistics refreshed: new=$newWords, inProgress=$inProgress, learned=$learned")

                // Здесь можно сохранить статистику или передать в UI
                // Например, через SettingsManager или отдельный StateFlow

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing statistics", e)
            }
        }
    }
    fun onKnowCard() {
        _uiState.value.currentWord?.let { word ->
            ServiceLocator.wordRepository.answerWord(word.id, known = true)
        }
    }

    fun onDontKnowCard() {
        _uiState.value.currentWord?.let { word ->
            ServiceLocator.wordRepository.answerWord(word.id, known = false)
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