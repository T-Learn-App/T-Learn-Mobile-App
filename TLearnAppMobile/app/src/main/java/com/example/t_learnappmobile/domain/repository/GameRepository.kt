// domain/repository/GameRepository.kt
package com.example.t_learnappmobile.domain.repository

import com.example.t_learnappmobile.domain.model.GameResult
import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.model.DailyStats
import com.example.t_learnappmobile.domain.model.LeaderboardPlayer

interface GameRepository {
    suspend fun loadGameWords(dictionaryId: String, limit: Int): List<GameWord>
    suspend fun saveGameResult(score: Int, totalWords: Int)
    suspend fun getGameResults(userId: String): List<GameResult>
    suspend fun getWeeklyStats(userId: String, weekOffset: Int): List<DailyStats>
    suspend fun getLeaderboard(limit: Int): List<LeaderboardPlayer>
    suspend fun getPlayerPosition(userId: String): LeaderboardPlayer?
}