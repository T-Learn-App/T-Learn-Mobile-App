package com.example.t_learnappmobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.t_learnappmobile.data.auth.models.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TokenManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)


    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        prefs.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
        }.apply()
    }

    suspend fun getAccessToken(): Flow<String?> = flow {
        emit(prefs.getString("access_token", null))
    }

    suspend fun getRefreshToken(): Flow<String?> = flow {
        emit(prefs.getString("refresh_token", null))
    }

    fun isTokenExpired(token: String): Boolean {
        return token.contains("expired") || token.isEmpty()
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }


    suspend fun saveUserData(userData: UserData) {
        prefs.edit().apply {
            putInt("user_id", userData.id)
            putString("user_email", userData.email)
            putString("user_first_name", userData.firstName)
            putString("user_last_name", userData.lastName)
        }.apply()
    }

    suspend fun getUserData(): Flow<UserData?> = flow {
        val id = prefs.getInt("user_id", 0)
        if (id == 0) {
            emit(null)
            return@flow
        }

        val email = prefs.getString("user_email", null) ?: return@flow
        val firstName = prefs.getString("user_first_name", null)
        val lastName = prefs.getString("user_last_name", null)

        emit(UserData(id, email, firstName, lastName))
    }
}
