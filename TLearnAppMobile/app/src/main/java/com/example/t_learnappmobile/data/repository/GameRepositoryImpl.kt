package com.example.t_learnappmobile.data.repository

import android.util.Log
import com.example.t_learnappmobile.data.local.GameLocalSource
import com.example.t_learnappmobile.data.local.LeaderboardCacheEntry
import com.example.t_learnappmobile.data.remote.FirebaseAuthSource
import com.example.t_learnappmobile.data.remote.FirebaseGameSource
import com.example.t_learnappmobile.domain.model.DailyStats
import com.example.t_learnappmobile.domain.model.GameResult
import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.model.LeaderboardPlayer
import com.example.t_learnappmobile.domain.repository.GameRepository
import kotlinx.coroutines.withTimeoutOrNull

class GameRepositoryImpl(
    private val gameSource: FirebaseGameSource,
    private val authSource: FirebaseAuthSource,
    private val gameLocalSource: GameLocalSource
) : GameRepository {

    private val TAG = "GameRepository"

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

        val result = withTimeoutOrNull(3000L) {
            gameSource.saveGameResult(userId, score, totalWords)
        }

        if (result == null) {
            gameLocalSource.savePendingGameResult(userId, score, totalWords)
        }
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
            val result = withTimeoutOrNull(5000L) {
                gameSource.getLeaderboard(limit)
            }

            if (result != null) {
                val cacheEntries = result.map { player ->
                    LeaderboardCacheEntry(
                        id = player.id,
                        name = player.name,
                        score = player.score,
                        position = player.position
                    )
                }
                gameLocalSource.cacheLeaderboard(cacheEntries)
                result
            } else {
                val cached = gameLocalSource.getCachedLeaderboard()
                cached?.map { entry ->
                    LeaderboardPlayer(
                        id = entry.id,
                        name = entry.name,
                        score = entry.score,
                        position = entry.position
                    )
                } ?: emptyList()
            }
        } catch (e: Exception) {
            val cached = gameLocalSource.getCachedLeaderboard()
            cached?.map { entry ->
                LeaderboardPlayer(
                    id = entry.id,
                    name = entry.name,
                    score = entry.score,
                    position = entry.position
                )
            } ?: emptyList()
        }
    }

    override suspend fun getPlayerPosition(userId: String): LeaderboardPlayer? {
        return try {
            withTimeoutOrNull(3000L) {
                gameSource.getPlayerPosition(userId)
            } ?: run {
                val cachedLeaderboard = gameLocalSource.getCachedLeaderboard()
                val cachedPosition = cachedLeaderboard?.find { it.id == userId }
                if (cachedPosition != null) {
                    LeaderboardPlayer(
                        id = cachedPosition.id,
                        name = cachedPosition.name,
                        score = cachedPosition.score,
                        position = cachedPosition.position
                    )
                } else {
                    val localScore = gameLocalSource.getLocalUserScore(userId)
                    if (localScore > 0) {
                        LeaderboardPlayer(
                            id = userId,
                            name = "",
                            score = localScore,
                            position = 999
                        )
                    } else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun syncPendingResults() {
        val userId = authSource.getCurrentUserId()
        if (userId == null) return

        val pendingResults = gameLocalSource.getPendingGameResults()
        if (pendingResults.isEmpty()) return

        Log.d(TAG, "Syncing ${pendingResults.size} pending game results")

        for (result in pendingResults) {
            try {
                gameSource.saveGameResult(result.userId, result.score, result.wordsCount)
                Log.d(TAG, "Synced result: score=${result.score}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync result", e)
            }
        }

        gameLocalSource.removePendingGameResults(pendingResults)
    }
}