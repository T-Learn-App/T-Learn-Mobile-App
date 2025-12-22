package com.example.t_learnappmobile.data.auth

import com.example.t_learnappmobile.R
import com.example.t_learnappmobile.data.auth.models.*
import com.example.t_learnappmobile.presentation.auth.AuthState
import kotlinx.coroutines.flow.firstOrNull

class AuthRepository(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager,

) {


    suspend fun checkAuthState(): AuthState {
        return try {
            val token = tokenManager.getAccessToken().firstOrNull()
            if (token.isNullOrEmpty()) {
                return AuthState.LoggedOut
            }

            try {
                val response = apiService.ping()
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
        email: String,
        password: String
    ): AuthState {
        val request = RegisterRequest( email, password)
        val validation = request.validate()
        if (validation !is ValidationResult.Success) {
            return AuthState.Error(R.string.error_validation, arrayOf((validation as ValidationResult.Error).message))
        }

        return try {
            if (checkEmailExists(email)) {
                return AuthState.Error(R.string.error_login_already_exists)
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
                    AuthState.Error(R.string.error_email_or_login_used)
                }

                response.code() == 400 -> {
                    AuthState.Error(R.string.error_server_error)
                }

                else -> {
                    AuthState.Error(R.string.error_registration_failed, arrayOf(response.code()))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AuthState.Error(R.string.error_unknown_registration)
        }
    }


    suspend fun login(
        email: String,
        password: String
    ): AuthState {
        val request = LoginRequest(email, password)

        val validation = request.validate()
        if (validation !is ValidationResult.Success) {
            return AuthState.Error(R.string.error_validation, arrayOf((validation as ValidationResult.Error).message))
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
                    AuthState.Error(R.string.error_login_or_password_incorrect)
                }

                response.code() == 403 -> {
                    AuthState.Error(R.string.error_email_not_verified)
                }

                else -> {
                    AuthState.Error(R.string.error_login_failed, arrayOf(response.code()))
                }
            }
        } catch (e: Exception) {
            AuthState.Error(R.string.error_network)
        }
    }

    suspend fun logout(): AuthState {
        try {
            apiService.logout()
        } catch (e: NoNetworkException) {
            return AuthState.Error(R.string.error_no_internet)
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

}
