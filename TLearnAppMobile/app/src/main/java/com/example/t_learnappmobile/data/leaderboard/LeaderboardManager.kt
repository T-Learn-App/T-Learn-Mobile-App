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
                name = getUserFullName(result.userId),  // ✅ Теперь из JWT!
                score = result.totalScore,
                position = index + 1
            )
        }
        _players.value = playersWithPositions
        return playersWithPositions
    }

    private suspend fun getUserFullName(userId: Int): String {
        // 🔥 ИЗ JWT ТОКЕНА!
        val currentUserId = ServiceLocator.tokenManager.getUserId()?.toInt() ?: 0
        return if (currentUserId == userId) {
            val email = ServiceLocator.tokenManager.getUserEmail() ?: "Игрок"
            email.split("@").first()  // "test" из "test@example.com"
                .replaceFirstChar { it.uppercase() }
                .take(10) + if (email.length > 10) "..." else ""
        } else {
            "Игрок $userId"
        }
    }

    suspend fun getYourPosition(userId: Int): LeaderboardPlayer {
        val totalScore = ServiceLocator.gameResultDao.getUserTotalScore(userId)
        val info = ServiceLocator.gameResultDao.getUserLeaderboardInfo(userId)
        val currentUserId = ServiceLocator.tokenManager.getUserId()?.toInt() ?: userId

        return LeaderboardPlayer(
            id = userId,
            name = getUserFullName(currentUserId),  // ✅ JWT!
            score = totalScore,
            position = info?.position ?: 0
        )
    }
}
