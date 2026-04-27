package com.example.t_learnappmobile.data.auth

import com.example.t_learnappmobile.data.auth.models.LoginRequest
import com.example.t_learnappmobile.presentation.auth.AuthState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import android.util.Log

class AuthRepository(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    companion object {
        private const val SERVER_TIMEOUT_MS = 8000L
    }

    private fun validateCredentials(email: String, password: String): AuthState? {
        if (email.isBlank()) return AuthState.Error("Почта не может быть пустой")
        if (!email.contains("@")) return AuthState.Error("Введите корректную почту")
        if (password.isBlank()) return AuthState.Error("Пароль не может быть пустым")
        if (password.length < 8) return AuthState.Error("Пароль слишком короткий (мин. 8 символов)")

        if (!password.any { it.isLowerCase() }) return AuthState.Error("Пароль должен содержать строчные буквы")
        if (!password.any { it.isUpperCase() }) return AuthState.Error("Пароль должен содержать заглавные буквы")
        if (!password.any { it.isDigit() }) return AuthState.Error("Пароль должен содержать цифры")
        if (!password.any { "!@#\$%^&*()_+=-[]{}|;:,.<>?".contains(it) })
            return AuthState.Error("Пароль должен содержать спецсимволы")

        return null
    }

    suspend fun login(email: String, password: String): AuthState {
        val validationError = validateCredentials(email, password)
        if (validationError != null) return validationError

        return try {
            val serverResult = withTimeoutOrNull(SERVER_TIMEOUT_MS) {
                val request = LoginRequest(email, password, null, null)
                val response = apiService.login(request)

                when (response.code()) {
                    200 -> {
                        val auth = response.body()
                        if (auth?.accessToken.isNullOrEmpty()) {
                            return@withTimeoutOrNull AuthState.Error("Ошибка сервера: нет токена")
                        }

                        tokenManager.saveTokens(auth!!.accessToken, auth.refreshToken)
                        val userId = tokenManager.getUserId()
                        val userEmail = tokenManager.getUserEmail()

                        AuthState.Success(userId = userId ?: 1L, email = userEmail ?: email)
                    }
                    400 -> AuthState.Error("Неверная почта или пароль")
                    401 -> AuthState.Error("Неверные данные для входа")
                    403 -> AuthState.Error("Аккаунт заблокирован")
                    429 -> AuthState.Error("Слишком много попыток. Попробуйте позже")
                    else -> null
                }
            }

            serverResult ?: AuthState.Error("Сервер недоступен. Проверьте интернет")
        } catch (e: Exception) {
            AuthState.Error("Ошибка сети. Проверьте подключение")
        }
    }

    suspend fun register(email: String, password: String): AuthState {
        val validationError = validateCredentials(email, password)
        if (validationError != null) return validationError

        return try {
            val serverResult = withTimeoutOrNull(SERVER_TIMEOUT_MS) {
                val request = LoginRequest(email, password, null, null)
                val response = apiService.login(request)

                when (response.code()) {
                    200, 201 -> {
                        val auth = response.body()
                        if (auth?.accessToken.isNullOrEmpty()) {
                            return@withTimeoutOrNull AuthState.Error("Ошибка сервера: нет токена")
                        }

                        tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                        val userId = tokenManager.getUserId()
                        val userEmail = tokenManager.getUserEmail()

                        AuthState.Success(userId = userId ?: 1L, email = userEmail ?: email)
                    }
                    400 -> AuthState.Error("Пользователь с такой почтой уже существует")
                    409 -> AuthState.Error("Пользователь уже существует")
                    422 -> AuthState.Error("Неверные данные для регистрации")
                    429 -> AuthState.Error("Слишком много попыток. Попробуйте позже")
                    else -> null
                }
            }

            serverResult ?: AuthState.Error("Сервер недоступен. Проверьте интернет")
        } catch (e: Exception) {
            AuthState.Error("Ошибка сети. Проверьте подключение")
        }
    }

    suspend fun logout(): AuthState {
        tokenManager.clearTokens()
        return AuthState.LoggedOut
    }

    suspend fun checkAuthState(): AuthState {
        val token = tokenManager.getAccessToken().firstOrNull()
        return if (token.isNullOrEmpty()) {
            AuthState.LoggedOut
        } else {
            val userId = tokenManager.getUserId()
            val userEmail = tokenManager.getUserEmail()
            AuthState.Success(userId = userId ?: 1L, email = userEmail ?: "unknown")
        }
    }
}