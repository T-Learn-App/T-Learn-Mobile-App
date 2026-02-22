package com.example.t_learnappmobile.data.leaderboard

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LeaderboardManager {
    private val _players = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val players: StateFlow<List<LeaderboardPlayer>> = _players

    suspend fun loadLeaderboard(): List<LeaderboardPlayer> {
        delay(500)
        val mockPlayers = listOf(
            LeaderboardPlayer(1, "Иван Иванов", 2450, position = 1),
            LeaderboardPlayer(2, "Анна Петрова", 1987, position = 2),
            LeaderboardPlayer(3, "Сергей Сидоров", 1678, position = 3),
            LeaderboardPlayer(4, "Мария Козлова", 1542, position = 4),
            LeaderboardPlayer(5, "Дмитрий Смирнов", 1321, position = 5),
            LeaderboardPlayer(6, "Елена Васильева", 987, position = 6),
            LeaderboardPlayer(7, "Алексей Морозов", 765, position = 7),
            LeaderboardPlayer(8, "Ольга Кузнецова", 543, position = 8),
            LeaderboardPlayer(9, "Михаил Попов", 432, position = 9),
            LeaderboardPlayer(10, "Татьяна Новикова", 321, position = 10)
        )
        _players.value = mockPlayers
        return mockPlayers
    }

    suspend fun getYourPosition(userId: Int): LeaderboardPlayer {
        delay(200)
        return LeaderboardPlayer(userId, "Mezo Alart", 1250, position = 12)
    }
}
