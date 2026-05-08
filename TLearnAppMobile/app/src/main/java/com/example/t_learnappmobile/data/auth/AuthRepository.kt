package com.example.t_learnappmobile.data.auth

import com.example.t_learnappmobile.presentation.auth.AuthState

class AuthRepository(
    private val firebaseAuthManager: FirebaseAuthManager
) {
    suspend fun login(email: String, password: String): AuthState {
        if (email.isBlank()) return AuthState.Error("Почта не может быть пустой")
        if (password.isBlank()) return AuthState.Error("Пароль не может быть пустым")
        if (password.length < 6) return AuthState.Error("Пароль должен быть минимум 6 символов")

        return firebaseAuthManager.login(email, password)
    }

    suspend fun register(email: String, password: String, firstName: String = "", lastName: String = ""): AuthState {
        if (email.isBlank()) return AuthState.Error("Почта не может быть пустой")
        if (password.length < 6) return AuthState.Error("Пароль должен быть минимум 6 символов")

        return firebaseAuthManager.register(email, password, firstName, lastName)
    }

     fun logout(): AuthState {
        firebaseAuthManager.logout()
        return AuthState.LoggedOut
    }

    fun checkAuthState(): AuthState {
        val user = firebaseAuthManager.getCurrentUser()
        return if (user != null) {
            AuthState.Success(
                userId = user.uid,  // Теперь String
                email = user.email
            )
        } else {
            AuthState.LoggedOut
        }
    }
}