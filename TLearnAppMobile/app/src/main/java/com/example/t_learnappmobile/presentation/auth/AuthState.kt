package com.example.t_learnappmobile.presentation.auth

import com.example.t_learnappmobile.data.auth.models.UserData

sealed class AuthState {
    data object Idle : AuthState()
    
    data object Loading : AuthState()
    data class Success(val user: UserData?) : AuthState()
    data class Error(val messageResId: Int, val args: Array<Any> = emptyArray()) : AuthState()
    data object LoggedOut : AuthState()
}
