package com.example.t_learnappmobile.data.leaderboard

import com.example.t_learnappmobile.data.repository.ServiceLocator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
class LeaderboardManager {
    private val _players = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val players: StateFlow<List<LeaderboardPlayer>> = _players

    suspend fun loadLeaderboard(): List<LeaderboardPlayer> {
        val results = ServiceLocator.gameResultDao.getLeaderboardWithUserIds()
        val playersWithPositions = results.mapIndexed { index, result ->
            LeaderboardPlayer(
                id = result.userId,
                name = getUserFullName(result.userId),
                score = result.totalScore,
                position = index + 1
            )
        }
        _players.value = playersWithPositions
        return playersWithPositions
    }

    private suspend fun getUserFullName(userId: Int): String {
        val userData = ServiceLocator.tokenManager.getUserData().firstOrNull()
        return if (userData?.id == userId) {
            "${userData.firstName ?: ""} ${userData.lastName ?: ""}".trim()
        } else {
            "Игрок $userId"
        }
    }

    suspend fun getYourPosition(userId: Int): LeaderboardPlayer {
        val totalScore = ServiceLocator.gameResultDao.getUserTotalScore(userId)
        val info = ServiceLocator.gameResultDao.getUserLeaderboardInfo(userId)
        val userData = ServiceLocator.tokenManager.getUserData().firstOrNull()

        return LeaderboardPlayer(
            id = userId,
            name = "${userData?.firstName ?: ""} ${userData?.lastName ?: ""}".trim().ifEmpty { "Игрок $userId" },
            score = totalScore,
            position = info?.position ?: 0
        )
    }
}
