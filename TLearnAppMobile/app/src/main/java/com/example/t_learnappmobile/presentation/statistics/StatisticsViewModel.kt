package com.example.t_learnappmobile.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.leaderboard.LeaderboardManager
import com.example.t_learnappmobile.data.leaderboard.LeaderboardPlayer
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.repository.StatDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class StatisticsViewModel : ViewModel() {
    private val tokenManager = ServiceLocator.tokenManager
    private val leaderboardManager = ServiceLocator.leaderboardManager
    private val wordApi = ServiceLocator.api

    data class DailyStats(
        val date: String,
        val inProgressWords: Int = 0,
        val learnedWords: Int = 0
    )

    data class TotalStats(
        val new: Int = 0,
        val inProgress: Int = 0,
        val learned: Int = 0
    )

    private val _weekStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val weekStats: StateFlow<List<DailyStats>> = _weekStats

    private val _totalStats = MutableStateFlow(TotalStats())
    val totalStats: StateFlow<TotalStats> = _totalStats

    private val _currentDictionaryName = MutableStateFlow("Все слова")
    val currentDictionaryName: StateFlow<String> = _currentDictionaryName

    private val _leaderboardPlayers = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val leaderboardPlayers: StateFlow<List<LeaderboardPlayer>> = _leaderboardPlayers

    private val _yourPosition = MutableStateFlow<LeaderboardPlayer?>(null)
    val yourPosition: StateFlow<LeaderboardPlayer?> = _yourPosition

    private val _isLeaderboardLoading = MutableStateFlow(false)
    val isLeaderboardLoading: StateFlow<Boolean> = _isLeaderboardLoading

    private fun loadLeaderboard() {
        viewModelScope.launch {
            try {
                _isLeaderboardLoading.value = true

                val userId = tokenManager.getUserId()
                val seasonId = currentSeasonId

                if (userId != null) {
                    leaderboardManager.loadLeaderboardWithMyPosition(seasonId, userId)
                } else {

                    leaderboardManager.loadLeaderboardOnly(seasonId)
                }


                viewModelScope.launch {
                    leaderboardManager.players.collect { players ->
                        _leaderboardPlayers.value = players
                    }
                }

                viewModelScope.launch {
                    leaderboardManager.yourPosition.collect { position ->
                        _yourPosition.value = position
                    }
                }

            } catch (e: Exception) {
                Log.e("StatisticsViewModel", "Failed to load leaderboard", e)
            } finally {
                _isLeaderboardLoading.value = false
            }
        }
    }


    fun refreshStats() {
        loadStatisticsFromBackend()
        loadLeaderboard()
    }

    private val _yourGameScore = MutableStateFlow(0)
    val yourGameScore: StateFlow<Int> = _yourGameScore

    private val _seasonText = MutableStateFlow("Сезон 1")
    val seasonText: StateFlow<String> = _seasonText

    private val currentSeasonId: String = "1"

    init {
        loadStatisticsFromBackend()
        loadLeaderboard()
    }



    private fun loadStatisticsFromBackend() {
        viewModelScope.launch {
            try {
                val accessToken = tokenManager.getAccessToken().firstOrNull()
                if (accessToken != null) {
                    val response = wordApi.getStats("Bearer $accessToken")

                    if (response.isSuccessful && response.body() != null) {
                        val statsList = response.body()!!.stats ?: emptyList()

                        var completed = 0
                        var inProgress = 0

                        statsList.forEach { stat ->
                            when (stat.status) {
                                "COMPLETED" -> completed++
                                "IN_PROGRESS" -> inProgress++
                            }
                        }

                        _totalStats.value = TotalStats(
                            new = 0,
                            inProgress = inProgress,
                            learned = completed
                        )

                        Log.d("StatisticsViewModel", "Stats loaded: completed=$completed, inProgress=$inProgress")
                        generateWeekStats()
                    }
                }
            } catch (e: Exception) {
                Log.e("StatisticsViewModel", "Failed to load stats from backend", e)
                _totalStats.value = TotalStats(0, 0, 0)
            }
        }
    }

    private fun generateWeekStats() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)

        val weekStats = mutableListOf<DailyStats>()
        repeat(7) {
            weekStats.add(
                DailyStats(
                    date = dateFormat.format(calendar.time),
                    inProgressWords = 0,
                    learnedWords = 0
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        _weekStats.value = weekStats
    }


}