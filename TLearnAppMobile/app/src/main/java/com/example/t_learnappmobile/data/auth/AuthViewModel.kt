package com.example.t_learnappmobile.data.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.presentation.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun register(login: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.register(login, email, password)
            _authState.value = result
        }
    }

    fun login(login: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(login, password)
            _authState.value = result
        }
    }

    fun sendVerificationCode(email: String) {
        viewModelScope.launch {
            val result = repository.sendVerificationCode(email)
            _authState.value = result
        }
    }

    fun verifyEmail(email: String, code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.verifyEmail(email, code)
            _authState.value = result
        }
    }



    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
