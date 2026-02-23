package com.example.t_learnappmobile.data.auth

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import kotlin.text.startsWith


class MockDispatcher : Dispatcher() {
    private val registeredUsers = mutableMapOf<String, UserMockData>()



    override fun dispatch(request: RecordedRequest): MockResponse {
        return when {

            request.path?.contains("auth/login") == true -> handleLogin(request)   // ← нужно
            request.path?.contains("auth/refresh") == true -> createRefreshResponse()
            request.path?.contains("auth/check-email") == true -> handleCheckEmail(request)
            else -> MockResponse().setResponseCode(404).setBody("""{"error":"Not found"}""")
        }
    }


    private fun handleLogin(request: RecordedRequest): MockResponse {
        return try {
            val body = request.body.readUtf8()
            val json = org.json.JSONObject(body)
            val email = json.getString("email")
            val password = json.getString("password")

            val userEntry = registeredUsers.entries.find {
                it.value.email == email && it.value.password == password
            }

            if (userEntry == null) {
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error":"Неверный логин или пароль"}""")
            } else {
                val userId = userEntry.key.toInt()
                createSuccessResponse(userId, userEntry.value.email)
            }
        } catch (e: Exception) {
            MockResponse()
                .setResponseCode(400)
                .setBody("""{"error":"Invalid request"}""")
        }
    }

    data class UserMockData(
        val email: String,
        val password: String,
        val firstName: String = "Игрок",
        val lastName: String = ""
    )



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

    private fun createSuccessResponse(
        userId: Int,
        email: String,
    ): MockResponse {
        return MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(
                """{
                "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIkdXNlcklkIiwiZXhwIjo5OTk5OTk5OTk5fQ.test",
                "refresh_token": "refresh_token_${System.currentTimeMillis()}",
                "user": {
                    "id": $userId,
                    "email": "$email"
            
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
                    "id": 12345,
                    "email": "user@example.com",
                }    
            }"""
            )
    }
}