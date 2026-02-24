package com.example.t_learnappmobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// data/auth/TokenManager.kt
class TokenManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

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

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    // 🔥 НОВЫЕ JWT МЕТОДЫ
    fun getUserId(): Long? {
        val token = prefs.getString("access_token", null) ?: return null
        return parseUserId(token)
    }

    fun getUserEmail(): String? {
        val token = prefs.getString("access_token", null) ?: return null
        return parseEmail(token)
    }

    fun isTokenExpired(token: String): Boolean {
        val payload = JwtParser.decodePayload(token) ?: return true
        val exp = payload["exp"] as? Long ?: return true
        return System.currentTimeMillis() / 1000 > exp
    }

    // Приватные парсеры
    private fun parseUserId(token: String): Long? {
        val payload = JwtParser.decodePayload(token) ?: return null
        return try {
            (payload["userId"] as? String)?.toLong()
        } catch (e: NumberFormatException) { null }
    }

    private fun parseEmail(token: String): String? {
        val payload = JwtParser.decodePayload(token) ?: return null
        return payload["sub"] as? String ?: payload["email"] as? String
    }
}

