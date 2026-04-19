package com.example.t_learnappmobile.data.user

import retrofit2.Response
import retrofit2.http.*

interface UserApiService {
    @GET("users/{id}")
    suspend fun getUserById(
        @Header("Authorization") authorization: String,
        @Path("id") userId: Long
    ): Response<UserResponse>

    @PUT("users/profile")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: UserProfileUpdateRequest
    ): Response<UserResponse>
}

data class UserProfileUpdateRequest(
    val firstName: String,
    val lastName: String
)

data class UserResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String
)