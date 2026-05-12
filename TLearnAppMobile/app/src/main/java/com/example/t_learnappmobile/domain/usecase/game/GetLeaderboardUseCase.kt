// domain/usecase/game/GetLeaderboardUseCase.kt
package com.example.t_learnappmobile.domain.usecase.game

import com.example.t_learnappmobile.domain.model.LeaderboardPlayer
import com.example.t_learnappmobile.domain.repository.GameRepository

class GetLeaderboardUseCase(
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(limit: Int = 100): List<LeaderboardPlayer> {
        return gameRepository.getLeaderboard(limit)
    }
}