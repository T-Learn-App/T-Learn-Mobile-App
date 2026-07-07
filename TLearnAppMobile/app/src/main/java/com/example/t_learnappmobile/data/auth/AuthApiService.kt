package com.example.t_learnappmobile.data.auth

import com.example.t_learnappmobile.data.auth.models.AuthResponse
import com.example.t_learnappmobile.data.auth.models.LoginRequest
import com.example.t_learnappmobile.data.auth.models.RefreshRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/token/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<AuthResponse>
}
