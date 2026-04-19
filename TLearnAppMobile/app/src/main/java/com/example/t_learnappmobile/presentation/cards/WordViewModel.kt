package com.example.t_learnappmobile.presentation.cards

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.repository.ServiceLocator.storage
import com.example.t_learnappmobile.domain.model.CardAction
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.model.CardType
import com.example.t_learnappmobile.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WordViewModel(application: Application) : AndroidViewModel(application) {

    private val dictionaryManager = ServiceLocator.dictionaryManager

    private val repository: WordRepository = ServiceLocator.wordRepository
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord

    private val _isTranslationHidden = MutableStateFlow(true)
    val isTranslationHidden: StateFlow<Boolean> = _isTranslationHidden

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    private val _isWordsEmpty = MutableStateFlow(false)
    val isWordsEmpty: StateFlow<Boolean> = _isWordsEmpty

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isNetworkAvailable.value = true
        }

        override fun onLost(network: Network) {
            _isNetworkAvailable.value = false
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isNetworkAvailable.value = hasInternet
        }
    }

    init {
        registerNetworkCallback()
        observeCurrentCard()
        observeDictionaryChanges()
        loadInitialWords()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onCleared() {
        super.onCleared()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun observeCurrentCard() {
        viewModelScope.launch {
            repository.getCurrentCardFlow().collect { word ->
                _currentWord.value = word
                _isTranslationHidden.value = true
            }
        }
    }


    private fun observeDictionaryChanges() {
        viewModelScope.launch {

            dictionaryManager.currentVocabularyIdFlow
                .filterNotNull()
                .collect { catId ->

                    loadWordsForCategory(catId.toLong())
                }
        }
    }


    private fun loadInitialWords() {
        viewModelScope.launch {
            val userId = getUserId()

            if (userId != null) {

                dictionaryManager.initVocabulary(userId)

                val initialCategory = dictionaryManager.getCurrentVocabularyId(userId).toLong()

                loadWordsForCategory(initialCategory)
            }
        }
    }


    fun fetchWords() {
        viewModelScope.launch {
            _error.value = null
            val userId = getUserId() ?: return@launch
            val categoryId = dictionaryManager.getCurrentVocabularyId(userId).toLong()
            loadWordsForCategory(categoryId)
        }
    }

    private suspend fun loadWordsForCategory(categoryId: Long) {


        if (!_isNetworkAvailable.value) {

            _error.value = "Нет соединения с интернетом"
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        _currentWord.value = null
        _error.value = null

        try {

            val words = repository.fetchWords(categoryId)


            _isWordsEmpty.value = words.isEmpty()

            if (words.isNotEmpty()) {
                storage.updateWords(words)


                repository.nextWord()

            }
        } catch (e: Exception) {

            _error.value = e.message ?: "Не удалось загрузить слова"
        } finally {
            _isLoading.value = false

        }
    }

    private fun formatTodayDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private suspend fun getUserId(): Int? {
        return ServiceLocator.tokenManager.getUserId()?.toInt() ?: 0
    }

    fun toggleTranslation() {
        _isTranslationHidden.value = !_isTranslationHidden.value
    }

    suspend fun onKnowCard() {
        val word = _currentWord.value ?: return
        repository.completeWord(word.id, CardAction.KNOW)

        val userId = getUserId() ?: return
        val today = formatTodayDate()
        val current = dictionaryManager.getDailyStats(userId, today)
        val updated = current.copy(learnedWords = current.learnedWords + 1)
        dictionaryManager.saveDailyStats(userId, updated)

        repository.nextWord()
    }

    suspend fun onDontKnowCard() {
        val word = _currentWord.value ?: return
        repository.completeWord(word.id, CardAction.DONT_KNOW)

        val userId = getUserId() ?: return
        val today = formatTodayDate()
        val current = dictionaryManager.getDailyStats(userId, today)
        val updated = current.copy(
            newWords = current.newWords + if (word.cardType == CardType.NEW) 1 else 0,
            inProgressWords = current.inProgressWords + if (word.cardType == CardType.ROTATION) 1 else 0
        )
        dictionaryManager.saveDailyStats(userId, updated)

        repository.nextWord()
    }
}
