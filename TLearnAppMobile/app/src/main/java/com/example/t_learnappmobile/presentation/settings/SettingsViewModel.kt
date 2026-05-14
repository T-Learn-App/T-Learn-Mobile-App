// presentation/settings/SettingsViewModel.kt
package com.example.t_learnappmobile.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.domain.model.Dictionary
import com.example.t_learnappmobile.domain.repository.AuthRepository
import com.example.t_learnappmobile.domain.repository.UserRepository
import com.example.t_learnappmobile.domain.repository.WordRepository
import com.example.t_learnappmobile.domain.usecase.settings.SettingsUseCase
import com.example.t_learnappmobile.domain.usecase.user.ResetUserDataUseCase
import com.example.t_learnappmobile.domain.usecase.user.UpdateProfileUseCase
import com.example.t_learnappmobile.domain.usecase.words.GetDictionariesUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val dictionaries: List<Dictionary> = emptyList(),
    val currentDictionaryId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val isDarkTheme: Boolean = false,
    val error: String? = null,
    val showResetDictionaryDialog: Boolean = false,
    val showResetAllDialog: Boolean = false
)

class SettingsViewModel(
    private val getDictionariesUseCase: GetDictionariesUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val resetUserDataUseCase: ResetUserDataUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val wordRepository: WordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    // presentation/settings/SettingsViewModel.kt
// Замените метод loadData:

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val dicts = getDictionariesUseCase()

                // Получаем сохраненный ID словаря
                val savedDictId = settingsUseCase.getCurrentDictionaryId()
                Log.d("SettingsVM", "Saved dictionary ID from settings: '$savedDictId'")

                // Если сохраненный ID есть и он есть в списке - используем его
                // Иначе берем первый из списка
                val currentDictId = if (!savedDictId.isNullOrEmpty() && dicts.any { it.id == savedDictId }) {
                    savedDictId
                } else {
                    dicts.firstOrNull()?.id ?: ""
                }

                Log.d("SettingsVM", "Using dictionary ID: '$currentDictId'")

                val userId = authRepository.getCurrentUserId()
                val profile = userId?.let { userRepository.getUserProfile(it) }
                val userEmail = authRepository.getUserEmail() ?: ""

                val isDarkTheme = settingsUseCase.isDarkTheme()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dictionaries = dicts,
                    currentDictionaryId = currentDictId,
                    firstName = profile?.firstName ?: "",
                    lastName = profile?.lastName ?: "",
                    email = userEmail,
                    isDarkTheme = isDarkTheme
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading data: ${e.message}"
                )
            }
        }
    }
    // presentation/settings/SettingsViewModel.kt
// Добавьте этот метод:

    fun loadCurrentDictionary() {
        val savedDictId = settingsUseCase.getCurrentDictionaryId()
        Log.d("SettingsVM", "loadCurrentDictionary: '$savedDictId'")
        if (!savedDictId.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(currentDictionaryId = savedDictId)
        }
    }
    fun refreshUserData() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val profile = userId?.let { userRepository.getUserProfile(it) }
                val userEmail = authRepository.getUserEmail() ?: ""

                _uiState.value = _uiState.value.copy(
                    firstName = profile?.firstName ?: "",
                    lastName = profile?.lastName ?: "",
                    email = userEmail
                )
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    fun updateDictionary(dictionaryId: String) {
        val dict = _uiState.value.dictionaries.find { it.id == dictionaryId } ?: return
        settingsUseCase.setCurrentDictionary(dictionaryId, dict.name)
        _uiState.value = _uiState.value.copy(currentDictionaryId = dictionaryId)
    }

    fun updateProfile(firstName: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isSuccess = false)

            updateProfileUseCase(firstName, lastName).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        firstName = firstName,
                        lastName = lastName,
                        isLoading = false,
                        isSuccess = true
                    )
                    delay(2000)
                    _uiState.value = _uiState.value.copy(isSuccess = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun updateTheme(isDarkTheme: Boolean) {
        settingsUseCase.setTheme(isDarkTheme)
        _uiState.value = _uiState.value.copy(isDarkTheme = isDarkTheme)
    }

    fun resetDictionaryStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val userId = authRepository.getCurrentUserId()
            val currentDictId = _uiState.value.currentDictionaryId

            if (userId != null && currentDictId.isNotEmpty()) {
                try {
                    // Сбрасываем прогресс только для текущего словаря
                    wordRepository.resetDictionaryProgress(userId, currentDictId)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                    delay(2000)
                    _uiState.value = _uiState.value.copy(isSuccess = false)
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
            val userId = authRepository.getCurrentUserId()

            if (userId != null) {
                try {
                    // Сбрасываем прогресс для всех словарей
                    wordRepository.resetAllProgress(userId)

                    // Сбрасываем настройки
                    settingsUseCase.clearAllSettings()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        firstName = "",
                        lastName = "",
                        email = "",
                        currentDictionaryId = ""
                    )
                    delay(2000)
                    _uiState.value = _uiState.value.copy(isSuccess = false)
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
}