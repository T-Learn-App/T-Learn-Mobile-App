package com.example.t_learnappmobile.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.domain.model.Dictionary
import com.example.t_learnappmobile.domain.repository.AuthRepository
import com.example.t_learnappmobile.domain.repository.UserRepository
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.domain.usecase.settings.SettingsUseCase
import com.example.t_learnappmobile.domain.usecase.user.UpdateProfileUseCase
import com.example.t_learnappmobile.domain.usecase.words.GetDictionariesUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val dictionaries: List<Dictionary> = emptyList(),
    val currentDictionaryId: String = "",
    val currentDictionaryName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val isDarkTheme: Boolean = false,
    val error: String? = null,
    val isInitialized: Boolean = false
)

class SettingsViewModel(
    private val getDictionariesUseCase: GetDictionariesUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val wordRepository: WordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            if (_uiState.value.isInitialized) return@launch

            _uiState.value = _uiState.value.copy(isLoading = true)

            val userId = authRepository.getCurrentUserId()
            val userEmail = authRepository.getUserEmail() ?: ""
            val isDarkTheme = settingsUseCase.isDarkTheme()

            val dicts = try {
                getDictionariesUseCase()
            } catch (e: Exception) {
                emptyList()
            }

            val savedDictId = settingsUseCase.getCurrentDictionaryId()
            val savedDictName = settingsUseCase.getCurrentDictionaryName()

            val currentDictId = if (!savedDictId.isNullOrEmpty() && dicts.any { it.id == savedDictId }) {
                savedDictId
            } else {
                dicts.firstOrNull()?.id ?: "finance"
            }

            val currentDictName = if (!savedDictName.isNullOrEmpty()) {
                savedDictName
            } else {
                dicts.find { it.id == currentDictId }?.name ?: "Финансы"
            }

            val profile = userId?.let {
                try {
                    withTimeoutOrNull(3000L) { userRepository.getUserProfile(it) }
                } catch (e: Exception) {
                    null
                }
            }

            _uiState.value = SettingsUiState(
                isLoading = false,
                isInitialized = true,
                dictionaries = dicts.ifEmpty {
                    listOf(
                        Dictionary("finance", "Финансы", 1),
                        Dictionary("conversational", "Разговорные слова", 2),
                        Dictionary("technology", "Технологии", 3),
                        Dictionary("slang", "Сленг", 4)
                    )
                },
                currentDictionaryId = currentDictId,
                currentDictionaryName = currentDictName,
                firstName = profile?.firstName ?: "",
                lastName = profile?.lastName ?: "",
                email = userEmail,
                isDarkTheme = isDarkTheme
            )
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isInitialized = false, isLoading = true)
            loadData()
        }
    }

    fun updateDictionary(dictionaryId: String) {
        val dict = _uiState.value.dictionaries.find { it.id == dictionaryId }
        if (dict != null) {
            settingsUseCase.setCurrentDictionary(dictionaryId, dict.name)
            _uiState.value = _uiState.value.copy(
                currentDictionaryId = dictionaryId,
                currentDictionaryName = dict.name
            )
        }
    }

    fun syncCurrentDictionaryFromExternal() {
        viewModelScope.launch {
            val savedDictId = settingsUseCase.getCurrentDictionaryId()
            val savedDictName = settingsUseCase.getCurrentDictionaryName()

            if (!savedDictId.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(
                    currentDictionaryId = savedDictId,
                    currentDictionaryName = savedDictName ?: ""
                )
            }
        }
    }

    fun updateProfile(firstName: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                updateProfileUseCase(firstName, lastName)
                _uiState.value = _uiState.value.copy(
                    firstName = firstName,
                    lastName = lastName,
                    isLoading = false,
                    isSuccess = true
                )
                delay(1500)
                _uiState.value = _uiState.value.copy(isSuccess = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Ошибка обновления профиля"
                )
                delay(2000)
                _uiState.value = _uiState.value.copy(error = null)
            }
        }
    }

    fun updateTheme(isDarkTheme: Boolean) {
        settingsUseCase.setTheme(isDarkTheme)
        _uiState.value = _uiState.value.copy(isDarkTheme = isDarkTheme)
    }

    fun resetDictionaryStatistics(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val userId = authRepository.getCurrentUserId()
            val currentDictId = _uiState.value.currentDictionaryId

            if (userId != null && currentDictId.isNotEmpty()) {
                try {
                    // Сбрасываем только прогресс слов, не трогаем настройки
                    wordRepository.resetDictionaryProgressAndSync(userId, currentDictId)
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    onComplete()
                    delay(1500)
                    _uiState.value = _uiState.value.copy(isSuccess = false)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка сброса прогресса"
                    )
                    delay(2000)
                    _uiState.value = _uiState.value.copy(error = null)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Пользователь не авторизован"
                )
                delay(2000)
                _uiState.value = _uiState.value.copy(error = null)
            }
        }
    }

    fun resetAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val userId = authRepository.getCurrentUserId()

            if (userId != null) {
                try {

                    wordRepository.resetAllProgressAndSync(userId)

                    val currentTheme = _uiState.value.isDarkTheme
                    val currentDictId = _uiState.value.currentDictionaryId
                    val currentDictName = _uiState.value.currentDictionaryName

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        // Восстанавливаем настройки
                        isDarkTheme = currentTheme,
                        currentDictionaryId = currentDictId,
                        currentDictionaryName = currentDictName
                    )
                    onComplete()
                    delay(1500)
                    _uiState.value = _uiState.value.copy(isSuccess = false)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Ошибка сброса данных"
                    )
                    delay(2000)
                    _uiState.value = _uiState.value.copy(error = null)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Пользователь не авторизован"
                )
                delay(2000)
                _uiState.value = _uiState.value.copy(error = null)
            }
        }
    }
}