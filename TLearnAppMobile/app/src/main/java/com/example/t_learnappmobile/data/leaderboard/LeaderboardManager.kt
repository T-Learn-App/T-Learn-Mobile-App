package com.example.t_learnappmobile.data.leaderboard

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LeaderboardManager(
    private val api: LeaderboardApi = ServiceLocator.leaderboardApi
) {
    private val _players = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val players: StateFlow<List<LeaderboardPlayer>> = _players.asStateFlow()

    private val _yourPosition = MutableStateFlow<LeaderboardPlayer?>(null)
    val yourPosition: StateFlow<LeaderboardPlayer?> = _yourPosition.asStateFlow()

    private val _seasonText = MutableStateFlow("Сезон 1 (22.02-22.03)")
    val seasonText: StateFlow<String> = _seasonText.asStateFlow()

    private val _currentSeasonId = MutableStateFlow("2026-spring")
    val currentSeasonId: StateFlow<String> = _currentSeasonId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun loadLeaderboard(seasonId: String, userId: Long? = null) {
        try {
            _isLoading.update { true }

            _currentSeasonId.update { seasonId }

            val response = api.getLeaderboard(seasonId, userId)

            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    _players.update { data.leaderboard }
                    _yourPosition.update { data.currentUser }

                    Log.d("LeaderboardManager", "Loaded ${data.leaderboard.size} players")
                    if (data.currentUser != null) {
                        Log.d("LeaderboardManager", "Current user position: ${data.currentUser.position}, score: ${data.currentUser.score}")
                    }
                }
            } else {
                Log.e("LeaderboardManager", "Error: ${response.code()} - ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("LeaderboardManager", "Exception loading leaderboard", e)
        } finally {
            _isLoading.update { false }
        }
    }

    suspend fun loadLeaderboardOnly(seasonId: String) {
        loadLeaderboard(seasonId, userId = null)
    }

    suspend fun loadLeaderboardWithMyPosition(seasonId: String, userId: Long) {
        loadLeaderboard(seasonId, userId = userId)
    }

    fun getMyPosition(): LeaderboardPlayer? = _yourPosition.value

    fun getUserPosition(userId: Long): LeaderboardPlayer? {
        return _players.value.find { it.id.toLong() == userId }
    }

    fun updateSeasonText(startDate: String, endDate: String) {
        _seasonText.update { "Сезон 1 ($startDate-$endDate)" }
    }

    fun clear() {
        _players.update { emptyList() }
        _yourPosition.update { null }
        _isLoading.update { false }
    }
}