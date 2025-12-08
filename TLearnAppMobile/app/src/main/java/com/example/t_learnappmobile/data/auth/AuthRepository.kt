package com.example.t_learnappmobile.data.auth

import android.content.Context
import android.util.Log
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.data.auth.models.*
import com.example.t_learnappmobile.presentation.auth.AuthState
import kotlinx.coroutines.flow.firstOrNull

class AuthRepository(private val context: Context) {
    private val apiService = ApiClient.getAuthApiService(context)
    private val tokenManager = TokenManager(context)

    suspend fun checkAuthState(): AuthState {
        return try {
            val token = tokenManager.getAccessToken().firstOrNull()

            if (token.isNullOrEmpty()) {
                return AuthState.LoggedOut
            }

            if (tokenManager.isTokenExpired(token)) {
                val refreshToken = tokenManager.getRefreshToken().firstOrNull()
                if (refreshToken != null) {
                    return refreshTokenIfNeeded(refreshToken)
                }
                return AuthState.LoggedOut
            }

            val userData = tokenManager.getUserData().firstOrNull()
            AuthState.Success(userData)
        } catch (e: Exception) {
            AuthState.LoggedOut
        }
    }

    private suspend fun refreshTokenIfNeeded(refreshToken: String): AuthState {
        return try {
            val response = apiService.refreshToken(RefreshTokenRequest(refreshToken))

            when {
                response.isSuccessful && response.body() != null -> {
                    val authResponse = response.body()!!
                    tokenManager.saveTokens(
                        authResponse.accessToken,
                        authResponse.refreshToken
                    )
                    tokenManager.saveUserData(authResponse.user)
                    AuthState.Success(authResponse.user)
                }
                else -> {
                    tokenManager.clearTokens()
                    AuthState.LoggedOut
                }
            }
        } catch (e: Exception) {
            tokenManager.clearTokens()
            AuthState.LoggedOut
        }
    }

    suspend fun register(
        login: String,
        email: String,
        password: String
    ): AuthState {
        val request = RegisterRequest(login, email, password)
        val validation = request.validate()
        if (validation !is ValidationResult.Success) {
            return AuthState.Error((validation as ValidationResult.Error).message)
        }

        return try {
            if (checkLoginExists(login)) {
                return AuthState.Error(context.getString(R.string.error_login_already_exists))
            }
            if (checkEmailExists(email)) {
                return AuthState.Error(context.getString(R.string.error_email_already_registered))
            }

            val response = apiService.register(request)

            when {
                response.isSuccessful && response.body() != null -> {
                    sendVerificationCode(email)
                }
                response.code() == 409 -> {
                    AuthState.Error(context.getString(R.string.error_email_or_login_used))
                }
                response.code() == 400 -> {
                    val error = parseErrorResponse(response.errorBody()?.string())
                    AuthState.Error(error)
                }
                else -> {
                    AuthState.Error(context.getString(R.string.error_registration_failed, response.code()))
                }
            }
        } catch (e: Exception) {
            AuthState.Error(e.message ?: context.getString(R.string.error_unknown_registration))
        }
    }

    suspend fun sendVerificationCode(email: String): AuthState {
        if (!email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))) {
            return AuthState.Error(context.getString(R.string.error_invalid_email))
        }

        return try {
            val response = apiService.sendVerificationCode(
                mapOf("email" to email)
            )

            when {
                response.isSuccessful && response.body() != null -> {
                    val body = response.body()!!
                    AuthState.VerificationCodeSent(body.expiresIn)
                }
                response.code() == 404 -> {
                    AuthState.Error(context.getString(R.string.error_email_not_found_alt))
                }
                response.code() == 429 -> {
                    AuthState.Error(context.getString(R.string.error_too_many_attempts))
                }
                else -> {
                    AuthState.Error(context.getString(R.string.error_send_code_failed, response.code()))
                }
            }
        } catch (e: Exception) {
            AuthState.Error(context.getString(R.string.error_network_check_internet))
        }
    }

    suspend fun verifyEmail(
        email: String,
        code: String
    ): AuthState {
        if (code.isBlank() || code.length != 6) {
            return AuthState.Error(context.getString(R.string.error_code_must_be_6_digits))
        }

        return try {
            val response = apiService.verifyEmail(
                EmailVerificationRequest(email, code)
            )

            when {
                response.isSuccessful && response.body() != null -> {
                    val authResponse = response.body()!!
                    tokenManager.saveTokens(
                        authResponse.accessToken,
                        authResponse.refreshToken
                    )
                    tokenManager.saveUserData(authResponse.user)

                    AuthState.Success(authResponse.user)
                }
                response.code() == 400 -> {
                    AuthState.Error(context.getString(R.string.error_invalid_or_expired_code))
                }
                response.code() == 404 -> {
                    AuthState.Error(context.getString(R.string.error_email_not_found))
                }
                else -> {
                    AuthState.Error(context.getString(R.string.error_verification_failed, response.code()))
                }
            }
        } catch (e: Exception) {

            AuthState.Error(context.getString(R.string.error_network))
        }
    }

    suspend fun login(
        login: String,
        password: String
    ): AuthState {
        val request = LoginRequest(login, password)

        val validation = request.validate()
        if (validation !is ValidationResult.Success) {
            return AuthState.Error((validation as ValidationResult.Error).message)
        }

        return try {
            val response = apiService.login(request)

            when {
                response.isSuccessful && response.body() != null -> {
                    val authResponse = response.body()!!
                    tokenManager.saveTokens(
                        authResponse.accessToken,
                        authResponse.refreshToken
                    )
                    tokenManager.saveUserData(authResponse.user)
                    AuthState.Success(authResponse.user)
                }
                response.code() == 401 -> {
                    AuthState.Error(context.getString(R.string.error_login_or_password_incorrect))
                }
                response.code() == 403 -> {
                    AuthState.Error(context.getString(R.string.error_email_not_verified))
                }
                else -> {
                    AuthState.Error(context.getString(R.string.error_login_failed, response.code()))
                }
            }
        } catch (e: Exception) {
            AuthState.Error(context.getString(R.string.error_network))
        }
    }


    private suspend fun checkEmailExists(email: String): Boolean {
        return try {
            val response = apiService.checkEmailExists(mapOf("email" to email))
            response.body()?.get("exists") ?: false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkLoginExists(login: String): Boolean {
        return try {
            val response = apiService.checkLoginExists(mapOf("login" to login))
            response.body()?.get("exists") ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun parseErrorResponse(errorBody: String?): String {
        return try {
            if (errorBody == null) return context.getString(R.string.error_unknown)
            val json = org.json.JSONObject(errorBody)
            json.optString("error_description",
                json.optString("message", context.getString(R.string.error_server_error)))
        } catch (e: Exception) {
            context.getString(R.string.error_processing_response)
        }
    }
}
