// data/repository/GameRepositoryImpl.kt
package com.example.t_learnappmobile.data.repository

import com.example.t_learnappmobile.data.remote.FirebaseAuthSource
import com.example.t_learnappmobile.data.remote.FirebaseGameSource
import com.example.t_learnappmobile.domain.model.DailyStats
import com.example.t_learnappmobile.domain.model.GameResult
import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.model.LeaderboardPlayer
import com.example.t_learnappmobile.domain.repository.GameRepository
import com.example.t_learnappmobile.domain.repository.UserRepository

class GameRepositoryImpl(
    private val gameSource: FirebaseGameSource,
    private val authSource: FirebaseAuthSource,
    userRepository: UserRepository
) : GameRepository {

    override suspend fun loadGameWords(dictionaryId: String, limit: Int): List<GameWord> {
        return gameSource.loadGameWords(dictionaryId, limit)
    }

    override suspend fun saveGameResult(score: Int, totalWords: Int) {
        val userId = authSource.getCurrentUserId() ?: return
        // Сохраняем только результат игры, без обновления счета
        gameSource.saveGameResult(userId, score, totalWords)
    }

    override suspend fun getGameResults(userId: String): List<GameResult> {
        return gameSource.getGameResults(userId)
    }

    override suspend fun getWeeklyStats(userId: String, weekOffset: Int): List<DailyStats> {
        return gameSource.getWeeklyStats(userId, weekOffset)
    }

    override suspend fun getLeaderboard(limit: Int): List<LeaderboardPlayer> {
        return gameSource.getLeaderboard(limit)
    }

    override suspend fun getPlayerPosition(userId: String): LeaderboardPlayer? {
        return gameSource.getPlayerPosition(userId)
    }
}