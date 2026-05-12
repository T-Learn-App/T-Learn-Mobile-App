package com.example.t_learnappmobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val repository = ServiceLocator.authRepository

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState(isLoading = true)
            val result = repository.login(email, password)
            _authState.value = when (result) {
                is AuthState.Success -> {
                    // ✅ После успешного входа синхронизируем данные
                    val syncManager = ServiceLocator.syncManager
                    syncManager.syncAllData()
                    syncManager.startPeriodicSync()
                    AuthUiState(isSuccess = true)
                }
                is AuthState.Error -> AuthUiState(error = result.message)
                else -> AuthUiState(error = "Неизвестная ошибка")
            }
        }
    }

    fun register(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState(isLoading = true)
            val result = repository.register(email, password, firstName, lastName)
            _authState.value = when (result) {
                is AuthState.Success -> AuthUiState(isSuccess = true)
                is AuthState.Error -> AuthUiState(error = result.message)
                else -> AuthUiState(error = "Неизвестная ошибка")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthUiState()
        }
    }

    fun checkAuthState() {
        viewModelScope.launch {
            val result = repository.checkAuthState()
            _authState.value = when (result) {
                is AuthState.Success -> AuthUiState(isSuccess = true)
                else -> AuthUiState()
            }
        }
    }

    fun resetState() {
        _authState.value = AuthUiState()
    }
}