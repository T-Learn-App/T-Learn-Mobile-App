// domain/repository/AuthRepository.kt
package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.domain.model.AuthResult

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthResult>
    suspend fun register(email: String, password: String, firstName: String, lastName: String): Result<AuthResult>
    suspend fun logout()
    fun getCurrentUserId(): String?
    fun getUserEmail(): String?
    fun isAuthenticated(): Boolean
}