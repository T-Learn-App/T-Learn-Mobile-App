// domain/usecase/game/SaveGameResultUseCase.kt
package com.example.t_learnappmobile.domain.usecase.game

import com.example.t_learnappmobile.domain.repository.GameRepository
import com.example.t_learnappmobile.domain.repository.UserRepository

class SaveGameResultUseCase(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(score: Int, totalWords: Int) {
        // Сохраняем результат игры
        gameRepository.saveGameResult(score, totalWords)

        // Обновляем общий счет пользователя ТОЛЬКО здесь
        if (score > 0) {
            userRepository.updateScore(score)
        }
    }
}