package com.example.t_learnappmobile.data.leaderboard

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull

class LeaderboardManager {
    private val _players = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val players: StateFlow<List<LeaderboardPlayer>> = _players

    private val _yourPosition = MutableStateFlow<LeaderboardPlayer?>(null)
    val yourPosition: StateFlow<LeaderboardPlayer?> = _yourPosition

    private val _seasonText = MutableStateFlow("Сезон 1")
    val seasonText: StateFlow<String> = _seasonText

    suspend fun loadLeaderboard(seasonId: String = "2026-spring") {
        _seasonText.value = "Сезон 1"
        try {
            val accessToken = ServiceLocator.tokenManager.getAccessToken().firstOrNull()
            if (accessToken == null) {
                return
            }
            val authHeader = "Bearer $accessToken"


            val response = ServiceLocator.gameApiService.getLeaderboard(authHeader, seasonId)
            if (response.isSuccessful && response.body() != null) {
                val serverPlayers = response.body()!!.players.mapIndexed { index, playerResp ->
                    LeaderboardPlayer(
                        id = playerResp.id.toInt(),
                        name = playerResp.name,
                        score = playerResp.score,
                        position = index + 1
                    )
                }
                _players.value = serverPlayers
            }


            loadYourPosition(seasonId)
        } catch (e: Exception) {

        }
    }

    private suspend fun loadYourPosition(seasonId: String) {
        try {
            val accessToken = ServiceLocator.tokenManager.getAccessToken().firstOrNull()
            if (accessToken == null) {
                return
            }
            val authHeader = "Bearer $accessToken"



            val response = ServiceLocator.gameApiService.getMyLeaderboardPosition(authHeader, seasonId)

            if (response.isSuccessful && response.body() != null) {
                val info = response.body()!!
                val email = ServiceLocator.tokenManager.getUserEmail() ?: "user@email.com"
                val displayName = email.split("@").first()
                    .replaceFirstChar { it.uppercase() }

                _yourPosition.value = LeaderboardPlayer(
                    id = info.userId,
                    name = displayName,
                    score = info.totalScore,
                    position = if (info.position > 0) info.position else (_players.value.size + 1)
                )

            }
        } catch (e: Exception) {

        }
    }
}
