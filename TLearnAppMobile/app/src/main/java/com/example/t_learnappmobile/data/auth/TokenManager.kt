package com.example.t_learnappmobile.data.auth

import JwtParser.decodePayload
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import kotlin.collections.component1
import kotlin.collections.component2

class TokenManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
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
            ?.replace(Regex("\\s"), "")
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
        val token = prefs.getString("access_token", null)
        if (token.isNullOrEmpty()) {
            Log.d("TokenManager", "No token found")
            return null
        }

        if (token.contains("mock")) {
            return 1L
        }

        return try {
            val payload = JwtParser.decodePayload(token)
            if (payload == null) {
                Log.e("TokenManager", "Failed to decode payload")
                debugToken(token)
                return null
            }

            Log.d("TokenManager", "Payload keys: ${payload.keys}")

            val userId = when (val value = payload["userId"]) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> {
                    Log.e("TokenManager", "userId is of type: ${value?.javaClass?.simpleName}")
                    null
                }
            }

            if (userId == null) {
                Log.e("TokenManager", "userId not found in token. Available keys: ${payload.keys}")
            } else {
                Log.d("TokenManager", "Successfully extracted userId: $userId")
            }

            userId
        } catch (e: Exception) {
            Log.e("TokenManager", "Error getting userId", e)
            null
        }
    }

    fun getUserEmail(): String? {
        val token = prefs.getString("access_token", null) ?: return null
        val payload = JwtParser.decodePayload(token)
        return payload?.get("email") as? String ?: payload?.get("sub") as? String
    }

}



fun debugToken(token: String) {
    android.util.Log.d("JwtParser", "=== DEBUG TOKEN ===")
    android.util.Log.d("JwtParser", "Raw token: '$token'")
    android.util.Log.d("JwtParser", "Length: ${token.length}")

    val cleanToken = token.trim().replace(Regex("\\s"), "")
    android.util.Log.d("JwtParser", "Clean token: '$cleanToken'")

    val parts = cleanToken.split(".")
    android.util.Log.d("JwtParser", "Parts count: ${parts.size}")

    if (parts.size == 3) {
        android.util.Log.d("JwtParser", "Header: ${parts[0]}")
        android.util.Log.d("JwtParser", "Payload: ${parts[1]}")
        android.util.Log.d("JwtParser", "Signature: ${parts[2].take(20)}...")

        val decoded = decodePayload(token)
        android.util.Log.d("JwtParser", "Decoded payload keys: ${decoded?.keys}")
        decoded?.forEach { (key, value) ->
            android.util.Log.d("JwtParser", "  $key = $value")
        }
    }
}

