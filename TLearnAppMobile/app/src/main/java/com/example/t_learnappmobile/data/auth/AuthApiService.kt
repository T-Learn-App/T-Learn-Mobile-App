package com.example.t_learnappmobile.data.auth

import com.example.t_learnappmobile.data.auth.models.AuthResponse
import com.example.t_learnappmobile.data.auth.models.LoginRequest
import com.example.t_learnappmobile.data.auth.models.LogoutResponse
import com.example.t_learnappmobile.data.auth.models.RegisterRequest
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

    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<LogoutResponse>

    @POST("auth/check-email")
    suspend fun checkEmailExists(
        @Body request: Map<String, String>
    ): Response<Map<String, Boolean>>

    @POST("auth/check-login")
    suspend fun checkLoginExists(
        @Body request: Map<String, String>
    ): Response<Map<String, Boolean>>

    @GET("auth/ping")
    suspend fun ping(@Header("Authorization") token: String): Response<Unit>

}
