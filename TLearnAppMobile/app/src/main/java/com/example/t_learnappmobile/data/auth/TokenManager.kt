package com.example.t_learnappmobile.data.auth

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.auth0.jwt.JWT
import com.example.t_learnappmobile.data.auth.models.UserData
import java.util.*

private val Context.dataStore by preferencesDataStore(name = "auth_tokens")

class TokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_LOGIN_KEY = stringPreferencesKey("user_login")
        private val USER_IS_VERIFIED_KEY = stringPreferencesKey("user_is_verified")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            if (refreshToken != null) {
                preferences[REFRESH_TOKEN_KEY] = refreshToken
            }
        }
    }

    suspend fun saveUserData(user: UserData) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = user.id
            preferences[USER_EMAIL_KEY] = user.email
            preferences[USER_LOGIN_KEY] = user.login
            preferences[USER_IS_VERIFIED_KEY] = user.isVerified.toString()
        }

    }

    fun getAccessToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }
    }

    fun getRefreshToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN_KEY]
        }
    }

    fun getUserData(): Flow<UserData?> {
        return context.dataStore.data.map { preferences ->
            val id = preferences[USER_ID_KEY]
            if (id != null) {
                UserData(
                    id = id,
                    email = preferences[USER_EMAIL_KEY] ?: "",
                    login = preferences[USER_LOGIN_KEY] ?: "",
                    isVerified = preferences[USER_IS_VERIFIED_KEY]?.toBoolean() ?: false
                )
            } else {
                null
            }
        }
    }

    fun isTokenExpired(token: String): Boolean {
        return try {
            val decodedJWT = JWT.decode(token)
            val expiresAt = decodedJWT.expiresAt
            expiresAt?.before(Date()) ?: false
        } catch (e: Exception) {
            true
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_EMAIL_KEY)
            preferences.remove(USER_LOGIN_KEY)
            preferences.remove(USER_IS_VERIFIED_KEY)
        }
    }
}
