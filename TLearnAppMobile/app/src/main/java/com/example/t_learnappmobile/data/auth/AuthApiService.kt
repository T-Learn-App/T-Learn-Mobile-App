package com.example.t_learnappmobile.data.auth

import com.example.t_learnappmobile.data.auth.models.AuthResponse
import com.example.t_learnappmobile.data.auth.models.LoginRequest
import com.example.t_learnappmobile.data.auth.models.RefreshRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

//    @POST("auth/login")
//    suspend fun register(
//        @Body request: RegisterRequest
//    ): Response<AuthResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("token/refresh")
    suspend fun refresh(@Body request: RefreshRequest): Response<AuthResponse>  // AuthRequest с refreshToken

//    @POST("auth/logout")
//    suspend fun logout(): Response<LogoutResponse>

//    @POST("auth/check-email")
//    suspend fun checkEmailExists(
//        @Body request: Map<String, String>
//    ): Response<Map<String, Boolean>>
//
//    @GET("auth/ping")
//    suspend fun ping(): Response<Unit>

}
