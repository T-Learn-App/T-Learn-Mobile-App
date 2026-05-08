package com.example.t_learnappmobile.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.settings.SettingsManager
import com.example.t_learnappmobile.model.Dictionary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val dictionaries: List<Dictionary> = emptyList(),
    val currentDictionaryId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",  // ДОБАВЛЕНО: поле для email
    val isDarkTheme: Boolean = false,
    val error: String? = null
)

class SettingsViewModel : ViewModel() {
    private val firestore = ServiceLocator.firestore
    private val authManager = ServiceLocator.firebaseAuthManager
    private val wordRepository = ServiceLocator.wordRepository
    private val settingsManager = ServiceLocator.appContext?.let {
        SettingsManager(it)
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * ИСПРАВЛЕНО: Загружаем данные ТОЛЬКО из Firebase для текущего пользователя
     * Больше не используем кэшированные данные из SettingsManager
     */
    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Load dictionaries
                val dicts = wordRepository.getDictionaries()
                val currentDictId = settingsManager?.getCurrentCategoryId() ?: dicts.firstOrNull()?.id ?: ""

                // ИСПРАВЛЕНО: Загружаем профиль ТОЛЬКО из Firebase через UserRepository
                val profile = ServiceLocator.userRepository.getCurrentUserProfile()

                // ИСПРАВЛЕНО: Получаем email напрямую из FirebaseAuth
                val userEmail = authManager.getUserEmail() ?: ""

                // ИСПРАВЛЕНО: Используем данные из Firebase, а не из кэша
                val firstName = profile?.firstName ?: ""
                val lastName = profile?.lastName ?: ""

                // Load theme preference
                val theme = settingsManager?.getTheme() ?: SettingsManager.THEME_SYSTEM
                val isDarkTheme = theme == SettingsManager.THEME_DARK

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dictionaries = dicts,
                    currentDictionaryId = currentDictId,
                    firstName = firstName,
                    lastName = lastName,
                    email = userEmail,  // ДОБАВЛЕНО: сохраняем email
                    isDarkTheme = isDarkTheme
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки данных: ${e.message}"
                )
            }
        }
    }

    /**
     * НОВЫЙ МЕТОД: Принудительно обновляет данные пользователя
     * Вызывается при возвращении на экран настроек
     */
    fun refreshUserData() {
        viewModelScope.launch {
            try {
                val profile = ServiceLocator.userRepository.getCurrentUserProfile()
                val userEmail = authManager.getUserEmail() ?: ""

                _uiState.value = _uiState.value.copy(
                    firstName = profile?.firstName ?: "",
                    lastName = profile?.lastName ?: "",
                    email = userEmail
                )
            } catch (e: Exception) {
                // Игнорируем ошибки при обновлении
            }
        }
    }

    fun updateDictionary(dictionaryId: String) {
        settingsManager?.setCurrentCategoryId(dictionaryId)
        _uiState.value = _uiState.value.copy(currentDictionaryId = dictionaryId)
    }

    fun updateProfile(firstName: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isSuccess = false)

            try {
                val success = settingsManager?.updateUserProfile(firstName, lastName) ?: false
                if (success) {
                    // ИСПРАВЛЕНО: Обновляем UI только после успешного сохранения
                    _uiState.value = _uiState.value.copy(
                        firstName = firstName,
                        lastName = lastName,
                        isLoading = false,
                        isSuccess = true
                    )
                    // Сбрасываем флаг успеха через 2 секунды
                    delay(2000)
                    _uiState.value = _uiState.value.copy(isSuccess = false)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Ошибка сохранения профиля"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка: ${e.message}"
                )
            }
        }
    }

    fun updateTheme(isDarkTheme: Boolean) {
        val theme = if (isDarkTheme) SettingsManager.THEME_DARK else SettingsManager.THEME_LIGHT
        settingsManager?.setTheme(theme)
        _uiState.value = _uiState.value.copy(isDarkTheme = isDarkTheme)
    }

    fun resetDictionaryStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val userId = authManager.getUserId()
            val currentDictId = _uiState.value.currentDictionaryId

            if (userId != null && currentDictId.isNotEmpty()) {
                try {
                    val wordsSnapshot = firestore.collection("words")
                        .whereEqualTo("dictionaryId", currentDictId)
                        .get()
                        .await()

                    for (doc in wordsSnapshot.documents) {
                        val userWordDocId = "${userId}_${doc.id}"  // ИСПРАВЛЕНО: правильный ID документа
                        firestore.collection("user_words")
                            .document(userWordDocId)
                            .set(
                                mapOf(
                                    "userId" to userId,
                                    "wordId" to doc.id,
                                    "dictionaryId" to currentDictId,
                                    "stage" to 0,
                                    "nextReviewDate" to System.currentTimeMillis(),
                                    "lastReviewDate" to null,
                                    "totalViews" to 0,
                                    "correctCount" to 0,
                                    "incorrectCount" to 0,
                                    "failCount" to 0
                                )
                            )
                            .await()
                    }
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Ошибка сброса: ${e.message}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Пользователь не авторизован"
                )
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val userId = authManager.getUserId()

            if (userId != null) {
                try {
                    // Reset all words progress for current user
                    val userWordsSnapshot = firestore.collection("user_words")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()

                    for (doc in userWordsSnapshot.documents) {
                        doc.reference.delete().await()
                    }

                    // Reset game results
                    val gamesSnapshot = firestore.collection("game_results")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()
                    for (doc in gamesSnapshot.documents) {
                        doc.reference.delete().await()
                    }

                    // Reset leaderboard
                    firestore.collection("leaderboard").document(userId).delete().await()

                    // Reset user score
                    firestore.collection("users").document(userId).update("totalScore", 0).await()

                    // Reset settings
                    settingsManager?.clearAllData()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        firstName = "",
                        lastName = "",
                        email = ""  // ДОБАВЛЕНО: очищаем email
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Ошибка сброса: ${e.message}"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Пользователь не авторизован"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}