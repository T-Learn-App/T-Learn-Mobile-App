// presentation/auth/AuthViewModel.kt
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
                onSuccess = { _uiState.value = AuthUiState(isSuccess = true) },
                onFailure = { e -> _uiState.value = AuthUiState(error = e.message) }
            )
        }
    }

    fun register(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            registerUseCase(email, password, firstName, lastName).fold(
                onSuccess = { _uiState.value = AuthUiState(isSuccess = true) },
                onFailure = { e -> _uiState.value = AuthUiState(error = e.message) }
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
        // Проверяем, авторизован ли пользователь
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