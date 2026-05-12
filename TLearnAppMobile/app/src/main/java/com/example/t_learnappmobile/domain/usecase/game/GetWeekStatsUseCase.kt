// domain/usecase/game/GetWeeklyStatsUseCase.kt
package com.example.t_learnappmobile.domain.usecase.game

import com.example.t_learnappmobile.domain.model.DailyStats
import com.example.t_learnappmobile.domain.repository.GameRepository

class GetWeeklyStatsUseCase(
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(userId: String, weekOffset: Int): List<DailyStats> {
        return gameRepository.getWeeklyStats(userId, weekOffset)
    }
}