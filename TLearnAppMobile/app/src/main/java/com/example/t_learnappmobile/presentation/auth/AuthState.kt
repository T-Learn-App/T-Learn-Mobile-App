package com.example.t_learnappmobile.presentation.auth

import com.example.t_learnappmobile.data.auth.models.UserData

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val user: UserData?) : AuthState()
    data class Error(val message: String) : AuthState()
    data class VerificationCodeSent(val expiresIn: Int) : AuthState()
    data object LoggedOut : AuthState()
}
