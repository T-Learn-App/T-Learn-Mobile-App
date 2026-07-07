package com.example.t_learnappmobile.data.user

import retrofit2.Response
import retrofit2.http.*

data class UserResponse(
    val id: Long,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class UserProfileUpdateRequest(
    val firstName: String,
    val lastName: String
)

interface UserApiService {
    @GET("users")
    suspend fun getCurrentUser(
        @Header("Authorization") authorization: String
    ): Response<UserResponse>

    @PUT("users/profile")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: UserProfileUpdateRequest
    ): Response<UserResponse>
}