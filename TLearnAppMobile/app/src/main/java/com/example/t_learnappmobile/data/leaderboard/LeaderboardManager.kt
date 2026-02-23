package com.example.t_learnappmobile.data.leaderboard

import com.example.t_learnappmobile.data.repository.ServiceLocator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LeaderboardManager {
    private val _players = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val players: StateFlow<List<LeaderboardPlayer>> = _players

    suspend fun loadLeaderboard(): List<LeaderboardPlayer> {
        val results = ServiceLocator.gameResultDao.getTopResults()
        val playersWithPositions = results
            .sortedByDescending { it.score }
            .take(10)
            .mapIndexed { index, result ->
                // TODO: получить имя пользователя по userId из API или локального кеша
                LeaderboardPlayer(
                    id = result.userId,
                    name = "Игрок ${result.userId}", // временно
                    score = result.score,
                    position = index + 1
                )
            }

        _players.value = playersWithPositions
        return playersWithPositions
    }

    suspend fun getYourPosition(userId: Int): LeaderboardPlayer {
        val bestScore = ServiceLocator.gameResultDao.getUserBestScore(userId)
        return LeaderboardPlayer(
            id = userId,
            name = "Mezo Alart",
            score = bestScore?.score ?: 0,
            position = 0 // позицию можно вычислить по всем результатам
        )
    }
}
