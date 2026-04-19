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

    private fun validateLogin(email: String, password: String): AuthState? {
        if (email.isBlank()) return AuthState.Error("Почта не может быть пустой")
        if (!email.contains("@")) return AuthState.Error("Введите корректную почту")
        if (password.isBlank()) return AuthState.Error("Пароль не может быть пустым")
        if (password.length < 6) return AuthState.Error("Пароль слишком короткий (мин. 6 символов)")
        return null
    }

    private fun validateRegister(email: String, password: String, firstName: String, lastName: String): AuthState? {
        val loginValidation = validateLogin(email, password)
        if (loginValidation != null) return loginValidation
        if (firstName.isBlank()) return AuthState.Error("Имя не может быть пустым")
        if (lastName.isBlank()) return AuthState.Error("Фамилия не может быть пустой")
        return null
    }

    suspend fun login(email: String, password: String): AuthState {

        val validationError = validateLogin(email, password)
        if (validationError != null) {
            return validationError
        }

        return try {
            val serverResult = withTimeoutOrNull(SERVER_TIMEOUT_MS) {
                val request = LoginRequest(email, password, null, null)
                val response = apiService.login(request)
                Log.e("AuthRepo", "Response code: ${response.code()}")
                val body = response.body()
                val error = response.errorBody()?.string()
                Log.e("AuthRepo", "Body: $body")
                Log.e("AuthRepo", "Error: $error")

                val auth = response.body()
                Log.e("AuthRepo", "!!! RAW TOKEN FROM SERVER: '${auth?.accessToken}'")

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
                    else -> {
                        null
                    }
                }
            }

            serverResult ?: AuthState.Error("Сервер недоступен. Проверьте интернет")

        } catch (e: Exception) {
            AuthState.Error("Ошибка сети. Проверьте подключение")
        }
    }

    suspend fun register(email: String, password: String, firstName: String, lastName: String): AuthState {


        val validationError = validateRegister(email, password, firstName, lastName)
        if (validationError != null) {
            return validationError
        }

        return try {
            val serverResult = withTimeoutOrNull(SERVER_TIMEOUT_MS) {

                val request = LoginRequest(email, password, firstName, lastName)
                val response = apiService.login(request)
                Log.e("AuthRepo", "Response code: ${response.code()}")
                val body = response.body()
                val error = response.errorBody()?.string()
                Log.e("AuthRepo", "Body: $body")
                Log.e("AuthRepo", "Error: $error")



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
                    else -> {

                        null
                    }
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