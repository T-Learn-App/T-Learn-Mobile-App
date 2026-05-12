// domain/repository/UserRepository.kt
package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.domain.model.UserProfile

interface UserRepository {
    suspend fun getUserProfile(userId: String): UserProfile?
    suspend fun createUserProfile(
        uid: String,
        email: String?,
        firstName: String,
        lastName: String
    ): Result<Unit>

    suspend fun updateProfile(firstName: String, lastName: String): Result<Unit>
    suspend fun updateScore(score: Int): Result<Unit>
    suspend fun resetUserData(userId: String): Result<Unit>
}