// data/repository/AuthRepositoryImpl.kt
package com.example.t_learnappmobile.data.repository

import com.example.t_learnappmobile.data.remote.FirebaseAuthSource
import com.example.t_learnappmobile.data.remote.FirebaseFirestoreSource
import com.example.t_learnappmobile.domain.model.AuthResult
import com.example.t_learnappmobile.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val authSource: FirebaseAuthSource,
    private val firestoreSource: FirebaseFirestoreSource
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthResult> {
        return authSource.signIn(email, password)
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<AuthResult> {
        // Только регистрация, профиль создается в RegisterUseCase
        return authSource.signUp(email, password)
    }

    override suspend fun logout() {
        authSource.signOut()
    }

    override fun getCurrentUserId(): String? = authSource.getCurrentUserId()
    override fun getUserEmail(): String? = authSource.getCurrentUserEmail()
    override fun isAuthenticated(): Boolean = authSource.isAuthenticated()
}