package com.example.t_learnappmobile.data.auth

import android.content.Context
import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.data.auth.models.*
import com.example.t_learnappmobile.presentation.auth.AuthState
import kotlinx.coroutines.flow.firstOrNull

class AuthRepository(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val context: Context
) {


    suspend fun checkAuthState(): AuthState {
        return try {
            val token = tokenManager.getAccessToken().firstOrNull()

            if (token.isNullOrEmpty()) {
                return AuthState.LoggedOut
            }

            try {
                val response = apiService.ping("Bearer $token")
                when {
                    response.isSuccessful -> {
                        val userData = tokenManager.getUserData().firstOrNull()
                        return AuthState.Success(userData)
                    }

                    response.code() == 401 -> {
                        tokenManager.clearTokens()
                        return AuthState.LoggedOut
                    }

                    else -> {
                        return AuthState.LoggedOut
                    }
                }
            } catch (e: Exception) {
                return AuthState.LoggedOut
            }

        } catch (e: Exception) {
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
                    val authResponse = response.body()!!
                    tokenManager.saveTokens(
                        authResponse.accessToken,
                        authResponse.refreshToken
                    )
                    tokenManager.saveUserData(authResponse.user)
                    AuthState.Success(authResponse.user)
                }

                response.code() == 409 -> {
                    AuthState.Error(context.getString(R.string.error_email_or_login_used))
                }

                response.code() == 400 -> {
                    val error = parseErrorResponse(response.errorBody()?.string())
                    AuthState.Error(error)
                }

                else -> {
                    AuthState.Error(
                        context.getString(
                            R.string.error_registration_failed,
                            response.code()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AuthState.Error(e.message ?: context.getString(R.string.error_unknown_registration))
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

    suspend fun logout(): AuthState {
        try {
            val token = tokenManager.getAccessToken().firstOrNull()
            apiService.logout("Bearer $token")
        } catch (e: NoNetworkException) {
            AuthState.Error(context.getString(R.string.error_no_internet))
        }
        tokenManager.clearTokens()
        return AuthState.LoggedOut
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
            json.optString(
                "error_description",
                json.optString("message", context.getString(R.string.error_server_error))
            )
        } catch (e: Exception) {
            context.getString(R.string.error_processing_response)
        }
    }
}
