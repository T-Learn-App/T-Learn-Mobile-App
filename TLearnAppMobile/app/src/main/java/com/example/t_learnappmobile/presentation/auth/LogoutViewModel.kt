package com.example.t_learnappmobile.presentation.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.auth.AuthRepository
import com.example.t_learnappmobile.data.repository.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LogoutViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ServiceLocator.authRepository

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.logout()
            _authState.value = result
        }
    }
}
