package com.example.t_learnappmobile.presentation.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.repository.ServiceLocator.authRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun register(email: String, password: String, firstName: String, lastName: String) {  // ✅ 4 параметра
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(email, password, firstName, lastName)
            _authState.value = result
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}


