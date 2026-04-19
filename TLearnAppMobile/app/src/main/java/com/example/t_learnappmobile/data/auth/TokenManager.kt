package com.example.t_learnappmobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class TokenManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        // Удаляем все пробельные символы (включая \n, \r, \t)
        val cleanAccess = accessToken.replace(Regex("\\s"), "")
        val cleanRefresh = refreshToken?.replace(Regex("\\s"), "")
        prefs.edit().apply {
            putString("access_token", cleanAccess)
            cleanRefresh?.let { putString("refresh_token", it) }
        }.apply()
        Log.d("TokenManager", "Saved clean token: ${cleanAccess.take(15)}...")
    }

    fun getAccessToken(): Flow<String?> = flow {
        val token = prefs.getString("access_token", null)
            ?.replace(Regex("\\s"), "") // повторная очистка при чтении
        emit(token)
    }

    fun getRefreshToken(): Flow<String?> = flow {
        val token = prefs.getString("refresh_token", null)
            ?.replace(Regex("\\s"), "")
        emit(token)
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    fun getUserId(): Long? {
        val token = prefs.getString("access_token", null) ?: return null
        if (token.contains("mock")) {
            return 1L
        }
        return try {
            val payload = JwtParser.decodePayload(token) ?: return null
            (payload["userId"] as? String)?.toLong()
        } catch (e: Exception) {
            null
        }
    }

    fun getUserEmail(): String? {
        val token = prefs.getString("access_token", null) ?: return null
        val payload = JwtParser.decodePayload(token)
        return payload?.get("email") as? String ?: payload?.get("sub") as? String
    }

}
