package com.example.t_learnappmobile.presentation.auth

sealed class AuthState {
    // Меняем userId: Long? -> String?
    data class Success(val userId: String?, val email: String?) : AuthState()
    data class Error(val message: String) : AuthState()
    object LoggedOut : AuthState()
}