package com.example.t_learnappmobile.data.auth

import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val getBaseUrl: () -> String
) : Interceptor {

    companion object {
        private val refreshLock = ReentrantLock()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        if (path.contains("auth")) {
            return chain.proceed(originalRequest)
        }

        val currentToken = getTokenSync()

        if (currentToken.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        var requestWithToken = addTokenToRequest(originalRequest, currentToken)
        Log.e("AuthInterceptor", "!!! Authorization header: '${requestWithToken.header("Authorization")}'")
        var response = chain.proceed(requestWithToken)

        if (response.code == 401) {
            response.close()
            val newToken = refreshAccessTokenSync()
            if (!newToken.isNullOrEmpty()) {
                requestWithToken = addTokenToRequest(originalRequest, newToken)
                response = chain.proceed(requestWithToken)
            }
        }

        return response
    }

    private fun getTokenSync(): String? = runBlocking {
        tokenManager.getAccessToken().firstOrNull()
    }

    private fun getRefreshTokenSync(): String? = runBlocking {
        tokenManager.getRefreshToken().firstOrNull()
    }

    private fun refreshAccessTokenSync(): String? = refreshLock.withLock {
        val refreshToken = getRefreshTokenSync() ?: return null
        return performTokenRefresh(refreshToken)
    }

    private fun performTokenRefresh(refreshToken: String): String? {
        return try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val body = """{"refreshToken": "$refreshToken"}""".toRequestBody(
                "application/json".toMediaType()
            )

            val request = okhttp3.Request.Builder()
                .url(getBaseUrl() + "auth/token/refresh")
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val newAccess = json.optString("accessToken")
                if (newAccess.isNotEmpty()) {
                    runBlocking {
                        tokenManager.saveTokens(newAccess, refreshToken)
                    }
                    return newAccess
                }
            }
            null
        } catch (e: Exception) {
            Log.e("AuthInterceptor", "Refresh failed", e)
            null
        }
    }

    private fun addTokenToRequest(request: Request, token: String): Request {
        val cleanToken = token.trim()
        return request.newBuilder()
            .header("Authorization", "Bearer $cleanToken")
            .build()
    }
}