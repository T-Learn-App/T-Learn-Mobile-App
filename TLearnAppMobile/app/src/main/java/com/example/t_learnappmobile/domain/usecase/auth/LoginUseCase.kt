package com.example.t_learnappmobile.domain.usecase.auth

import com.example.t_learnappmobile.domain.model.AuthResult
import com.example.t_learnappmobile.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthResult> {
        if (email.isBlank()) {
            return Result.failure(IllegalArgumentException("Введите email"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Пароль должен быть не менее 6 символов"))
        }

        return authRepository.login(email, password)
    }
}