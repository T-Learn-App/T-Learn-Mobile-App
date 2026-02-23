package com.example.t_learnappmobile.data.auth

import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.data.auth.models.LoginRequest
import com.example.t_learnappmobile.data.auth.models.RefreshRequest
import com.example.t_learnappmobile.data.auth.models.RegisterRequest
import com.example.t_learnappmobile.data.auth.models.UserData
import com.example.t_learnappmobile.data.auth.models.ValidationResult
import com.example.t_learnappmobile.presentation.auth.AuthState
import kotlinx.coroutines.flow.firstOrNull

class AuthRepository(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager,
) {

    
    companion object {
        private const val MOCK_MODE = true   // ← поставь false, если бэкенд работает
    }

    suspend fun checkAuthState(): AuthState {
        if (MOCK_MODE) {
            saveMockTokens()
            return AuthState.Success(UserData(id = 1, email = "test@example.com"))
        }

        val token = tokenManager.getAccessToken().firstOrNull() ?: return AuthState.LoggedOut

        // Нет ping → просто проверяем, есть ли токен и не истёк ли он
        if (tokenManager.isTokenExpired(token)) {
            tokenManager.clearTokens()
            return AuthState.LoggedOut
        }

        val userData = tokenManager.getUserData().firstOrNull()
        return if (userData != null) AuthState.Success(userData) else AuthState.LoggedOut
    }

    suspend fun login(email: String, password: String): AuthState {
        if (MOCK_MODE) {
            saveMockTokens()
            return AuthState.Success(UserData(id = 1, email = email))
        }

        val request = LoginRequest(email, password)
        // validate() — если есть, оставь; если нет — удали

        return try {
            val response = apiService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                // user теперь не приходит → сохраняем вручную или получаем отдельно
                tokenManager.saveUserData(UserData(id = 0, email = email)) // id пока 0, потом обнови
                AuthState.Success(UserData(id = 0, email = email))
            } else {
                when (response.code()) {
                    401 -> AuthState.Error(R.string.error_login_or_password_incorrect)
                    else -> AuthState.Error(R.string.error_login_failed, arrayOf(response.code().toString()))
                }
            }
        } catch (e: Exception) {
            AuthState.Error(R.string.error_network)
        }
    }suspend fun register(email: String, password: String, firstName: String, lastName: String): AuthState {
        if (MOCK_MODE) {
            val userId = (System.currentTimeMillis() % 1000 + 1).toInt()
            val userData = UserData(id = userId, email = email, firstName = firstName, lastName = lastName)
            tokenManager.saveTokens("mock_access_$userId", "mock_refresh_$userId")
            tokenManager.saveUserData(userData)
            return AuthState.Success(userData)
        }

        val request = RegisterRequest(email, password, firstName, lastName)

        return try {
            // ✅ Мок-регистрация (пока нет реального API)
            val userId = (System.currentTimeMillis() % 1000 + 1).toInt()
            val userData = UserData(id = userId, email = email, firstName = firstName, lastName = lastName)
            tokenManager.saveTokens("mock_register_access_$userId", "mock_register_refresh_$userId")
            tokenManager.saveUserData(userData)
            AuthState.Success(userData)

            // TODO: Раскомментировать для реального API

            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                tokenManager.saveUserData(UserData(id = 0, email = email, firstName = firstName, lastName = lastName))
                AuthState.Success(UserData(id = 0, email = email, firstName = firstName, lastName = lastName))
            } else {
                when (response.code()) {
                    409 -> AuthState.Error(R.string.error_network)
                    else -> AuthState.Error(R.string.error_registration_failed)
                }
            }

        } catch (e: Exception) {
            AuthState.Error(R.string.error_network)
        }
    }


    suspend fun refreshToken(): Boolean {
        val refreshToken = tokenManager.getRefreshToken().firstOrNull() ?: return false

        return try {
            val response = apiService.refresh(RefreshRequest(refreshToken))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                true
            } else {
                tokenManager.clearTokens()
                false
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            false
        }
    }

    suspend fun logout(): AuthState {
        tokenManager.clearTokens()
        return AuthState.LoggedOut
        // если бэкенд требует POST /logout — добавь позже
    }

    private suspend fun saveMockTokens() {
        tokenManager.saveTokens(
            accessToken = "fake-access-token",
            refreshToken = "fake-refresh-token"
        )
        tokenManager.saveUserData(UserData(id = 1, email = "test@example.com"))
    }
}