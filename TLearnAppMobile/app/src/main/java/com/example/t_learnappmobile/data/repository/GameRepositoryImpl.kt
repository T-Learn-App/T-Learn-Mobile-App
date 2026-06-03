package com.example.t_learnappmobile.data.repository

import com.example.t_learnappmobile.data.remote.FirebaseAuthSource
import com.example.t_learnappmobile.data.remote.FirebaseGameSource
import com.example.t_learnappmobile.domain.model.DailyStats
import com.example.t_learnappmobile.domain.model.GameResult
import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.model.LeaderboardPlayer
import com.example.t_learnappmobile.domain.repository.GameRepository

class GameRepositoryImpl(
    private val gameSource: FirebaseGameSource,
    private val authSource: FirebaseAuthSource
) : GameRepository {

    override suspend fun loadGameWords(dictionaryId: String, limit: Int): List<GameWord> {
        return try {
            gameSource.loadGameWords(dictionaryId, limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun saveGameResult(score: Int, totalWords: Int) {
        val userId = authSource.getCurrentUserId()
        if (userId == null) {
            throw IllegalStateException("User not authenticated")
        }
        gameSource.saveGameResult(userId, score, totalWords)
    }

    override suspend fun getGameResults(userId: String): List<GameResult> {
        return try {
            gameSource.getGameResults(userId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getWeeklyStats(userId: String, weekOffset: Int): List<DailyStats> {
        return try {
            gameSource.getWeeklyStats(userId, weekOffset)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getLeaderboard(limit: Int): List<LeaderboardPlayer> {
        return try {
            gameSource.getLeaderboard(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPlayerPosition(userId: String): LeaderboardPlayer? {
        return try {
            gameSource.getPlayerPosition(userId)
        } catch (e: Exception) {
            null
        }
    }
}