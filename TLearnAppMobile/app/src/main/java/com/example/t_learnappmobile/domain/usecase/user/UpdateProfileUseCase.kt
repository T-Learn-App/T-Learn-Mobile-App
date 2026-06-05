
package com.example.t_learnappmobile.domain.usecase.user

import com.example.t_learnappmobile.domain.repository.UserRepository
import kotlinx.coroutines.withTimeoutOrNull

class UpdateProfileUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(firstName: String, lastName: String): Result<Unit> {
        if (firstName.isBlank() && lastName.isBlank()) {
            return Result.failure(IllegalArgumentException("Имя или фамилия не могут быть пустыми"))
        }

        if (firstName.length > 20) {
            return Result.failure(IllegalArgumentException("Имя не может быть длиннее 20 символов"))
        }

        if (lastName.length > 20) {
            return Result.failure(IllegalArgumentException("Фамилия не может быть длиннее 20 символов"))
        }

        return try {
            val result = withTimeoutOrNull(5000L) {
                userRepository.updateProfile(firstName, lastName)
            }

            if (result != null) {
                result
            } else {
                Result.failure(Exception("Превышено время ожидания"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка сети. Проверьте подключение"))
        }
    }
}