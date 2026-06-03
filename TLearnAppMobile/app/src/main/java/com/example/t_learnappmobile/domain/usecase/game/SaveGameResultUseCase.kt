package com.example.t_learnappmobile.domain.usecase.game

import com.example.t_learnappmobile.domain.repository.GameRepository
import com.example.t_learnappmobile.domain.repository.UserRepository

class SaveGameResultUseCase(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(score: Int, totalWords: Int) {
        gameRepository.saveGameResult(score, totalWords)

        if (score > 0) {
            userRepository.updateScore(score)
        }
    }
}