package com.example.t_learnappmobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.domain.repository.AuthRepository
import com.example.t_learnappmobile.domain.usecase.auth.LoginUseCase
import com.example.t_learnappmobile.domain.usecase.auth.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            loginUseCase(email, password).fold(
                onSuccess = {
                    _uiState.value = AuthUiState(isSuccess = true)
                },
                onFailure = { e ->
                    val errorMessage = when {
                        e.message?.contains("Email cannot be empty") == true -> "Введите email"
                        e.message?.contains("Password must be at least 6 characters") == true -> "Пароль должен быть не менее 6 символов"
                        e.message?.contains("User not found") == true -> "Пользователь не найден"
                        e.message?.contains("Invalid password") == true -> "Неверный пароль"
                        e.message?.contains("Network error") == true -> "Ошибка сети. Проверьте подключение"
                        else -> e.message ?: "Ошибка входа"
                    }
                    _uiState.value = AuthUiState(error = errorMessage)
                }
            )
        }
    }

    fun register(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            registerUseCase(email, password, firstName, lastName).fold(
                onSuccess = {
                    _uiState.value = AuthUiState(isSuccess = true)
                },
                onFailure = { e ->
                    val errorMessage = when {
                        e.message?.contains("Email cannot be empty") == true -> "Введите email"
                        e.message?.contains("Email already in use") == true -> "Этот email уже зарегистрирован"
                        e.message?.contains("Password too short") == true -> "Пароль слишком короткий"
                        e.message?.contains("Минимум") == true -> e.message
                        else -> e.message ?: "Ошибка регистрации"
                    }
                    _uiState.value = AuthUiState(error = errorMessage)
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }

    fun checkAuthState() {
        if (authRepository.isAuthenticated()) {
            _uiState.value = AuthUiState(isSuccess = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}