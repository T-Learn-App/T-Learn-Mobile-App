package com.example.t_learnappmobile.presentation.auth



sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userId: Long?, val email: String?) : AuthState()
    data class Error(val message: String) : AuthState()
    object LoggedOut : AuthState()
}


