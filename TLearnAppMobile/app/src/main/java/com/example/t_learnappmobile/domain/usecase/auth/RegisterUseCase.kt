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

        // Валидация пароля
        val passwordErrors = mutableListOf<String>()
        if (password.length < 8) passwordErrors.add("Минимум 8 символов")
        if (!password.any { it.isUpperCase() }) passwordErrors.add("Заглавная буква")
        if (!password.any { it.isLowerCase() }) passwordErrors.add("Строчная буква")
        if (!password.any { it.isDigit() }) passwordErrors.add("Цифра")
        if (!password.any { "!@#\$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) passwordErrors.add("Спецсимвол")

        if (passwordErrors.isNotEmpty()) {
            return Result.failure(IllegalArgumentException(passwordErrors.joinToString("\n")))
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