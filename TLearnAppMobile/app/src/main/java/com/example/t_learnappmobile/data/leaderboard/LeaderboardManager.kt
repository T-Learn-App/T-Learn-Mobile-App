package com.example.t_learnappmobile.data.leaderboard

import com.example.t_learnappmobile.data.repository.ServiceLocator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
// LeaderboardManager.kt
class LeaderboardManager {
    private val _players = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val players: StateFlow<List<LeaderboardPlayer>> = _players

    suspend fun loadLeaderboard(): List<LeaderboardPlayer> {
        val results = ServiceLocator.gameResultDao.getLeaderboardWithUserIds()
        val playersWithPositions = results.mapIndexed { index, result ->
            LeaderboardPlayer(
                id = result.userId,
                name = getUserName(result.userId),
                score = result.totalScore,
                position = index + 1
            )
        }
        _players.value = playersWithPositions
        return playersWithPositions
    }

    suspend fun getYourPosition(userId: Int): LeaderboardPlayer {
        val totalScore = ServiceLocator.gameResultDao.getUserTotalScore(userId)
        val info = ServiceLocator.gameResultDao.getUserLeaderboardInfo(userId)

        return LeaderboardPlayer(
            id = userId,
            name = "Mezo Alart",
            score = totalScore,
            position = info?.position ?: 0
        )
    }

    private fun getUserName(userId: Int): String = when (userId) {
        1 -> "Mezo Alart"
        else -> "Игрок $userId"
    }
}
