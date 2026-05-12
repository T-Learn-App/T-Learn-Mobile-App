// domain/usecase/user/UpdateProfileUseCase.kt
package com.example.t_learnappmobile.domain.usecase.user

import com.example.t_learnappmobile.domain.repository.UserRepository

class UpdateProfileUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(firstName: String, lastName: String): Result<Unit> {
        if (firstName.isBlank() && lastName.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        return userRepository.updateProfile(firstName, lastName)
    }
}