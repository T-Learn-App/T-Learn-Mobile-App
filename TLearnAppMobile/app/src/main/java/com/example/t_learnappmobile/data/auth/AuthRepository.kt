package com.example.t_learnappmobile.data.auth


import android.util.Log
import com.example.t_learnappmobile.data.auth.models.LoginRequest
import com.example.t_learnappmobile.presentation.auth.AuthState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
// AuthRepository.kt - НОВАЯ ВЕРСИЯ
class AuthRepository(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    companion object {
        private const val SERVER_TIMEOUT_MS = 8000L
        private const val MOCK_USER_ID = 1L
    }

    // ✅ ВАЛИДАЦИЯ ПЕРЕД запросом
    private fun validateLogin(email: String, password: String): AuthState? {
        if (email.isBlank()) return AuthState.Error("Почта не может быть пустой")
        if (!email.contains("@")) return AuthState.Error("Введите корректную почту")
        if (password.isBlank()) return AuthState.Error("Пароль не может быть пустым")
        if (password.length < 6) return AuthState.Error("Пароль слишком короткий (мин. 6 символов)")
        return null
    }

    suspend fun login(email: String, password: String): AuthState {
        Log.d("🔐 AuthRepo", "🚀 LOGIN START: email=$email")

        // ✅ 1. CLIENT VALIDATION
        val validationError = validateLogin(email, password)
        if (validationError != null) {
            Log.w("🔐 AuthRepo", "❌ Client validation failed")
            return validationError
        }

        return try {
            // 🔥 2. Пробуем реальный сервер (БЕЗ MOCK!)
            val serverResult = withTimeoutOrNull(SERVER_TIMEOUT_MS) {
                Log.d("🔐 AuthRepo", "📤 POST /auth/login → $email")
                val request = LoginRequest(email, password)
                val response = apiService.login(request)

                Log.d("🔐 AuthRepo", "📡 Response: ${response.code()} | Body: ${response.body()}")

                // ✅ СТРОГАЯ ПРОВЕРКА СЕРВЕРА
                when (response.code()) {
                    200 -> {
                        val auth = response.body()
                        if (auth?.accessToken.isNullOrEmpty()) {
                            Log.e("🔐 AuthRepo", "❌ 200 но токен пустой!")
                            return@withTimeoutOrNull AuthState.Error("Ошибка сервера: нет токена")
                        }

                        Log.d("🔐 AuthRepo", "✅ TOKENS: access=${auth!!.accessToken.take(20)}...")
                        tokenManager.saveTokens(auth.accessToken, auth.refreshToken)

                        val userId = tokenManager.getUserId()
                        val userEmail = tokenManager.getUserEmail()
                        Log.d("🔐 AuthRepo", "👤 User: id=$userId, email=$userEmail")

                        AuthState.Success(userId = userId ?: 1L, email = userEmail ?: email)
                    }
                    400 -> AuthState.Error("Неверная почта или пароль")
                    401 -> AuthState.Error("Неверные данные для входа")
                    403 -> AuthState.Error("Аккаунт заблокирован")
                    429 -> AuthState.Error("Слишком много попыток. Попробуйте позже")
                    else -> {
                        Log.w("🔐 AuthRepo", "❌ Server error: ${response.code()}")
                        null  // Fallback timeout
                    }
                }
            }

            // ✅ 3. Если сервер не ответил → ОШИБКА (БЕЗ MOCK!)
            serverResult ?: AuthState.Error("Сервер недоступен. Проверьте интернет")

        } catch (e: Exception) {
            Log.e("🔐 AuthRepo", "💥 NETWORK ERROR: ${e.message}", e)
            AuthState.Error("Ошибка сети. Проверьте подключение")
        }
    }
    // В AuthRepository.kt добавь/исправь:
    suspend fun logout(): AuthState {
        Log.d("🔐 AuthRepo", "🚪 LOGOUT")
        tokenManager.clearTokens()
        Log.d("🔐 AuthRepo", "✅ Tokens cleared")
        return AuthState.LoggedOut
    }
    // В AuthRepository.kt добавь/проверь наличие:
    suspend fun checkAuthState(): AuthState {
        Log.d("🔐 AuthRepo", "🔍 checkAuthState()")
        val token = tokenManager.getAccessToken().firstOrNull()
        Log.d("🔐 AuthRepo", "📱 Token: ${if (token.isNullOrEmpty()) "EMPTY" else "OK"}")

        return if (token.isNullOrEmpty()) {
            Log.d("🔐 AuthRepo", "📴 Logged OUT")
            AuthState.LoggedOut
        } else {
            val userId = tokenManager.getUserId()
            val userEmail = tokenManager.getUserEmail()
            Log.d("🔐 AuthRepo", "✅ Logged IN: id=$userId, email=$userEmail")
            AuthState.Success(userId = userId ?: 1L, email = userEmail ?: "unknown")
        }
    }


}
