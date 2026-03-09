// data/auth/TokenManager.kt
package com.example.t_learnappmobile.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class TokenManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        Log.d("🔐 TokenManager", "💾 saveTokens")
        prefs.edit().apply {
            putString("access_token", accessToken)
            refreshToken?.let { putString("refresh_token", it) }
        }.apply()
    }

    fun getAccessToken(): Flow<String?> = flow {
        val token = prefs.getString("access_token", null)
        Log.d("🔐 TokenManager", "📱 getAccessToken: ${if (token.isNullOrEmpty()) "EMPTY" else "OK"}")
        emit(token)
    }

    fun getRefreshToken(): Flow<String?> = flow {
        val token = prefs.getString("refresh_token", null)
        Log.d("🔐 TokenManager", "🔄 getRefreshToken: ${if (token.isNullOrEmpty()) "EMPTY" else "OK"}")
        emit(token)
    }

    fun clearTokens() {
        Log.d("🔐 TokenManager", "🗑️ clearTokens")
        prefs.edit().clear().apply()
    }

    fun getUserId(): Long? {
        val token = prefs.getString("access_token", null) ?: return 1L
        if (token.contains("mock")) {
            Log.d("🔐 TokenManager", "🎭 Mock → userId=1")
            return 1L
        }
        return try {
            val payload = JwtParser.decodePayload(token) ?: return 1L
            (payload["userId"] as? String)?.toLong() ?: 1L
        } catch (e: Exception) {
            Log.w("🔐 TokenManager", "JWT parse failed", e)
            1L
        }
    }

    fun getUserEmail(): String? {
        val token = prefs.getString("access_token", null) ?: return null
        val payload = JwtParser.decodePayload(token)
        return payload?.get("email") as? String ?: payload?.get("sub") as? String
    }

    fun isTokenExpired(token: String): Boolean {
        val payload = JwtParser.decodePayload(token) ?: return true
        val exp = payload["exp"] as? Long ?: return true
        return System.currentTimeMillis() / 1000 > exp
    }
}
