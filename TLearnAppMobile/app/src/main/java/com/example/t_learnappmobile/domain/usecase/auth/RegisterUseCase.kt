// domain/usecase/auth/RegisterUseCase.kt
package com.example.t_learnappmobile.domain.usecase.auth

import com.example.t_learnappmobile.domain.model.AuthResult
import com.example.t_learnappmobile.domain.repository.AuthRepository
import com.example.t_learnappmobile.domain.repository.UserRepository

class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<AuthResult> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Email cannot be empty"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val result = authRepository.register(email, password, firstName, lastName)

        if (result.isSuccess) {
            val authResult = result.getOrNull()!!
            userRepository.createUserProfile(
                uid = authResult.userId,
                email = authResult.email,
                firstName = firstName,
                lastName = lastName
            )
        }

        return result
    }
}