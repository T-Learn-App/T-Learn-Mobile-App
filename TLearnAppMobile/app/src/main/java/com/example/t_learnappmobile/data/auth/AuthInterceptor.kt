package com.example.t_learnappmobile.data.auth

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
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

        if (originalRequest.url.encodedPath.contains("auth/refresh") ||
            originalRequest.url.encodedPath.contains("auth/login") ||
            originalRequest.url.encodedPath.contains("auth/register")
        ) {
            return chain.proceed(originalRequest)
        }

        val currentToken = getTokenSync()

        val requestWithToken = if (!currentToken.isNullOrEmpty()) {
            addTokenToRequest(originalRequest, currentToken)
        } else {
            originalRequest
        }

        var response = chain.proceed(requestWithToken)

        if (response.code == 401 && !currentToken.isNullOrEmpty()) {
            response.close()

            val newToken = refreshAccessTokenSync()
            if (!newToken.isNullOrEmpty()) {
                val newRequest = addTokenToRequest(originalRequest, newToken)
                response = chain.proceed(newRequest)
            }
        }

        return response
    }

    private fun getTokenSync(): String? {
        return try {
            runBlocking {
                tokenManager.getAccessToken().firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getRefreshTokenSync(): String? {
        return try {
            runBlocking {
                tokenManager.getRefreshToken().firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun refreshAccessTokenSync(): String? = refreshLock.withLock {
        val currentToken = getTokenSync()
        if (currentToken != null && !tokenManager.isTokenExpired(currentToken)) {
            return currentToken
        }

        val refreshToken = getRefreshTokenSync()
        if (refreshToken.isNullOrEmpty()) {
            runBlocking { tokenManager.clearTokens() }
            return null
        }

        return try {
            performTokenRefresh()
        } catch (e: Exception) {
            runBlocking { tokenManager.clearTokens() }
            null
        }
    }

    private fun performTokenRefresh(): String? {
        val refreshToken = getRefreshTokenSync() ?: return null

        return try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val body = """{"refreshToken": "$refreshToken"}""".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(getBaseUrl() + "/token/refresh")
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val json = org.json.JSONObject(response.body?.string() ?: "{}")
                val newAccess = json.optString("accessToken", null)
                val newRefresh = json.optString("refreshToken", refreshToken)

                if (!newAccess.isNullOrEmpty()) {
                    runBlocking { tokenManager.saveTokens(newAccess, newRefresh) }
                    return newAccess
                }
            }
            null
        } catch (e: Exception) {
            runBlocking { tokenManager.clearTokens() }
            null
        }
    }


    private fun addTokenToRequest(request: Request, token: String): Request {
        return request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }
}
