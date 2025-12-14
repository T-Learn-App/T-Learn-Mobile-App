package com.example.t_learnappmobile.data.auth

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import kotlin.text.startsWith


class MockDispatcher : Dispatcher() {
    private val registeredUsers = mutableMapOf<String, UserMockData>()

    data class UserMockData(
        val login: String,
        val email: String,
        val password: String,
        var isVerified: Boolean = false
    )

    override fun dispatch(request: RecordedRequest): MockResponse {
        return when {
            request.path?.contains("auth/register") == true -> handleRegister(request)
            request.path?.contains("auth/login") == true -> handleLogin(request)
            request.path?.contains("auth/refresh") == true -> createRefreshResponse()
            request.path?.contains("auth/check-email") == true -> handleCheckEmail(request)
            request.path?.contains("auth/check-login") == true -> handleCheckLogin(request)
            request.path?.contains("auth/me") == true -> handleGetCurrentUser(request)
            else -> MockResponse().setResponseCode(404).setBody("""{"error":"Not found"}""")
        }
    }

    private fun handleRegister(request: RecordedRequest): MockResponse {
        try {
            val body = request.body.readUtf8()
            val json = org.json.JSONObject(body)
            val login = json.getString("login")
            val email = json.getString("email")
            val password = json.getString("password")


            if (registeredUsers.values.any { it.email == email }) {
                return MockResponse()
                    .setResponseCode(409)
                    .setBody("""{"error":"Email уже используется"}""")
            }


            if (registeredUsers.values.any { it.login == login }) {
                return MockResponse()
                    .setResponseCode(409)
                    .setBody("""{"error":"Логин уже занят"}""")
            }

            val userId = "user_${System.currentTimeMillis()}"
            registeredUsers[userId] =
                UserMockData(login, email, password, true)  // ✅ is_verified = true

            return MockResponse()
                .setResponseCode(200)
                .setBody(
                    """
                {
                  "access_token": "mock_access_token_${userId}",
                  "refresh_token": "mock_refresh_token_${userId}",
                  "user": {
                    "id": "${userId}",
                    "login": "${login}",
                    "email": "${email}",
                    "is_verified": true
                  }
                }
            """.trimIndent()
                )

        } catch (e: Exception) {
            return MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"Invalid request"}""")
        }
    }


    private fun handleLogin(request: RecordedRequest): MockResponse {
        try {
            val body = request.body.readUtf8()
            val json = org.json.JSONObject(body)
            val login = json.getString("login")
            val password = json.getString("password")

            val user = registeredUsers.values.find {
                (it.login == login || it.email == login) && it.password == password
            }

            if (user == null) {
                return MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error":"Неверный логин или пароль"}""")
            }

            if (!user.isVerified) {
                return MockResponse()
                    .setResponseCode(403)
                    .setBody("""{"error":"Email не верифицирован"}""")
            }

            val userId = registeredUsers.entries.find { it.value == user }?.key ?: "unknown"
            return createSuccessResponse(userId, user.email, user.login, user.isVerified)
        } catch (e: Exception) {
            return MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"Invalid request"}""")
        }
    }


    private fun handleCheckEmail(request: RecordedRequest): MockResponse {
        try {
            val body = request.body.readUtf8()
            val json = org.json.JSONObject(body)
            val email = json.getString("email")
            val exists = registeredUsers.values.any { it.email == email }
            return MockResponse()
                .setResponseCode(200)
                .setBody("""{"exists":$exists}""")
        } catch (e: Exception) {
            return MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"Invalid request"}""")
        }
    }

    private fun handleCheckLogin(request: RecordedRequest): MockResponse {
        try {
            val body = request.body.readUtf8()
            val json = org.json.JSONObject(body)
            val login = json.getString("login")
            val exists = registeredUsers.values.any { it.login == login }
            return MockResponse()
                .setResponseCode(200)
                .setBody("""{"exists":$exists}""")
        } catch (e: Exception) {
            return MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"Invalid request"}""")
        }
    }

    private fun handleGetCurrentUser(request: RecordedRequest): MockResponse {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":"Unauthorized"}""")
        }

        return MockResponse()
            .setResponseCode(200)
            .setBody(
                """{
                "id": "12345",
                "email": "user@example.com",
                "login": "testuser",
                "is_verified": true
            }"""
            )
    }

    private fun createSuccessResponse(
        userId: String,
        email: String,
        login: String,
        isVerified: Boolean
    ): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(
                """{
                "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIkdXNlcklkIiwiZXhwIjo5OTk5OTk5OTk5fQ.test",
                "refresh_token": "refresh_token_${System.currentTimeMillis()}",
                "user": {
                    "id": "$userId",
                    "email": "$email",
                    "login": "$login",
                    "is_verified": $isVerified
                }
            }"""
            )
    }

    private fun createRefreshResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(
                """{
                "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuZXdfdG9rZW4iOiJ0cnVlIiwiZXhwIjo5OTk5OTk5OTk5fQ.test",
                "refresh_token": "refresh_token_new_${System.currentTimeMillis()}",
                "user": {
                    "id": "12345",
                    "email": "user@example.com",
                    "login": "testuser",
                    "is_verified": true
                }    
            }"""
            )
    }
}