package com.example.t_learnappmobile.data.auth

import android.util.Log
import com.example.t_learnappmobile.data.auth.models.LoginRequest
import com.example.t_learnappmobile.presentation.auth.AuthState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
class AuthRepository(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    companion object {
        private const val SERVER_TIMEOUT_MS = 8000L
        private const val MOCK_USER_ID = 1L
    }

    suspend fun login(email: String, password: String): AuthState {
        return try {
            // 🔥 1. Пробуем реальный сервер
            val serverResult = withTimeoutOrNull(SERVER_TIMEOUT_MS) {
                val request = LoginRequest(email, password)
                val response = apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                    return@withTimeoutOrNull AuthState.Success(
                        userId = tokenManager.getUserId(),
                        email = tokenManager.getUserEmail()
                    )
                }
                null
            }

            serverResult ?: throw Exception("Сервер недоступен")

        } catch (e: Exception) {
            android.util.Log.i("AuthRepository", "🔄 Fallback to MOCK: ${e.message}")

            // 🔥 2. Автоматический fallback на mock
            saveMockTokens(email)
            return AuthState.Success(
                userId = MOCK_USER_ID,
                email = email
            )
        }
    }

    suspend fun checkAuthState(): AuthState {
        val token = tokenManager.getAccessToken().firstOrNull()
        return if (token.isNullOrEmpty()) {
            AuthState.LoggedOut
        } else {
            AuthState.Success(
                userId = tokenManager.getUserId() ?: 1L,
                email = tokenManager.getUserEmail()
            )
        }
    }

    private suspend fun saveMockTokens(email: String) {
        val mockToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                "eyJ1c2VySWQiOiIxIiwiZW1haWwiOiIkemailIiwiZXhwIjo5OTk5OTk5OTk5fQ." +
                "mock-token-${System.currentTimeMillis()}"

        tokenManager.saveTokens(mockToken, "mock-refresh-${System.currentTimeMillis()}")
    }

    suspend fun logout(): AuthState {
        tokenManager.clearTokens()
        return AuthState.LoggedOut
    }
}

