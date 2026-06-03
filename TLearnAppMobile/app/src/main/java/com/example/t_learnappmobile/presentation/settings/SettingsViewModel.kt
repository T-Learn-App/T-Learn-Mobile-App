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
    val currentDictionaryName: String = "",
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

    fun refreshUserData() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val profile = userId?.let { userRepository.getUserProfile(it) }
                val userEmail = authRepository.getUserEmail() ?: ""

                val dicts = getDictionariesUseCase()
                val savedDictId = settingsUseCase.getCurrentDictionaryId()

                val currentDictId = if (!savedDictId.isNullOrEmpty() && dicts.any { it.id == savedDictId }) {
                    savedDictId
                } else {
                    dicts.firstOrNull()?.id ?: _uiState.value.currentDictionaryId
                }

                val currentDictName = dicts.find { it.id == currentDictId }?.name ?: ""

                _uiState.value = _uiState.value.copy(
                    firstName = profile?.firstName ?: "",
                    lastName = profile?.lastName ?: "",
                    email = userEmail,
                    dictionaries = dicts,
                    currentDictionaryId = currentDictId,
                    currentDictionaryName = currentDictName
                )
            } catch (e: Exception) {
                Log.e("SettingsVM", "Error refreshing user data", e)
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка обновления: ${e.message}"
                )
            }
        }
    }

    fun updateDictionary(dictionaryId: String) {
        val dict = _uiState.value.dictionaries.find { it.id == dictionaryId }
        if (dict != null) {
            Log.d("SettingsVM", "Updating dictionary in settings: ${dict.name} (${dict.id})")
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

            Log.d("SettingsVM", "syncCurrentDictionaryFromExternal: id='$savedDictId', name='$savedDictName'")

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
                    // Сбрасываем только прогресс текущего словаря
                    wordRepository.resetDictionaryProgress(userId, currentDictId)

                    // ✅ НЕ меняем профиль пользователя
                    // Просто показываем успех
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                        // firstName, lastName, email остаются без изменений
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
                    // Сбрасываем только прогресс слов
                    wordRepository.resetAllProgress(userId)

                    // Сбрасываем настройки (тема, выбранный словарь и т.д.)
                    settingsUseCase.clearAllSettings()

                    // ✅ НЕ сбрасываем профиль пользователя
                    // Загружаем актуальные данные профиля заново
                    val profile = userRepository.getUserProfile(userId)
                    val userEmail = authRepository.getUserEmail() ?: ""

                    // Загружаем словари заново
                    val dicts = getDictionariesUseCase()
                    val defaultDictId = dicts.firstOrNull()?.id ?: ""
                    val defaultDictName = dicts.firstOrNull()?.name ?: ""

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true,
                        firstName = profile?.firstName ?: "",      // ✅ сохраняем
                        lastName = profile?.lastName ?: "",        // ✅ сохраняем
                        email = userEmail,                          // ✅ сохраняем
                        dictionaries = dicts,
                        currentDictionaryId = defaultDictId,
                        currentDictionaryName = defaultDictName,
                        isDarkTheme = settingsUseCase.isDarkTheme() // ✅ сохраняем тему
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

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val dicts = getDictionariesUseCase()
                Log.d("SettingsVM", "Loaded ${dicts.size} dictionaries: ${dicts.map { "${it.id} (${it.name})" }}")

                val savedDictId = settingsUseCase.getCurrentDictionaryId()
                val savedDictName = settingsUseCase.getCurrentDictionaryName()

                val currentDictId = when {
                    !savedDictId.isNullOrEmpty() && dicts.any { it.id == savedDictId } -> savedDictId
                    dicts.isNotEmpty() -> dicts.first().id
                    else -> "default"
                }

                val currentDictName = when {
                    !savedDictName.isNullOrEmpty() -> savedDictName
                    else -> dicts.find { it.id == currentDictId }?.name ?: "Словарь"
                }

                Log.d("SettingsVM", "Using dictionary: id='$currentDictId', name='$currentDictName'")

                val userId = authRepository.getCurrentUserId()
                val profile = userId?.let { userRepository.getUserProfile(it) }
                val userEmail = authRepository.getUserEmail() ?: ""

                val isDarkTheme = settingsUseCase.isDarkTheme()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dictionaries = dicts,
                    currentDictionaryId = currentDictId,
                    currentDictionaryName = currentDictName,
                    firstName = profile?.firstName ?: "",
                    lastName = profile?.lastName ?: "",
                    email = userEmail,
                    isDarkTheme = isDarkTheme
                )

                Log.d("SettingsVM", "UI State updated: currentDictionaryId=${_uiState.value.currentDictionaryId}")
            } catch (e: Exception) {
                Log.e("SettingsVM", "Error loading data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dictionaries = emptyList(),
                    currentDictionaryId = "",
                    currentDictionaryName = "Ошибка загрузки",
                    error = "Ошибка загрузки данных: ${e.message}"
                )
            }
        }
    }
}