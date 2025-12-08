package com.example.t_learnappmobile.data.auth

import com.example.t_learnappmobile.data.auth.models.AuthResponse
import com.example.t_learnappmobile.data.auth.models.EmailVerificationRequest
import com.example.t_learnappmobile.data.auth.models.EmailVerificationResponse
import com.example.t_learnappmobile.data.auth.models.LoginRequest
import com.example.t_learnappmobile.data.auth.models.RefreshTokenRequest
import com.example.t_learnappmobile.data.auth.models.RegisterRequest
import com.example.t_learnappmobile.data.auth.models.UserData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header

interface AuthApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/email/send-verification")
    suspend fun sendVerificationCode(
        @Body request: Map<String, String>
    ): Response<EmailVerificationResponse>

    @POST("auth/email/verify")
    suspend fun verifyEmail(
        @Body request: EmailVerificationRequest
    ): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Map<String, String>>

    @POST("auth/check-email")
    suspend fun checkEmailExists(
        @Body request: Map<String, String>
    ): Response<Map<String, Boolean>>

    @POST("auth/check-login")
    suspend fun checkLoginExists(
        @Body request: Map<String, String>
    ): Response<Map<String, Boolean>>

    @GET("auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<UserData>
}
