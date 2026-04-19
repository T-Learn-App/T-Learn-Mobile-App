package com.example.t_learnappmobile.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.leaderboard.LeaderboardManager
import com.example.t_learnappmobile.data.leaderboard.LeaderboardPlayer
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.statistics.DailyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class StatisticsViewModel : ViewModel() {
    private val dictionaryManager = ServiceLocator.dictionaryManager
    private val tokenManager = ServiceLocator.tokenManager
    private val leaderboardManager = LeaderboardManager()
    private val wordApi = ServiceLocator.api

    private val _weekStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val weekStats: StateFlow<List<DailyStats>> = _weekStats

    private val _totalStats = MutableStateFlow(TotalStats(0, 0, 0))
    val totalStats: StateFlow<TotalStats> = _totalStats

    private val _currentDictionaryName = MutableStateFlow("Conversational")
    val currentDictionaryName: StateFlow<String> = _currentDictionaryName


    private val _leaderboardPlayers = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val leaderboardPlayers: StateFlow<List<LeaderboardPlayer>> = _leaderboardPlayers

    private val _yourPosition = MutableStateFlow<LeaderboardPlayer?>(null)
    val yourPosition: StateFlow<LeaderboardPlayer?> = _yourPosition
    
    private val _yourGameScore = MutableStateFlow(0)
    val yourGameScore: StateFlow<Int> = _yourGameScore
    
    private val _seasonText = MutableStateFlow("Сезон 1 (22.02-22.03)")
    val seasonText: StateFlow<String> = _seasonText
    private val currentSeasonId: String = "2026-spring"

    init {

        loadServerStats()
        loadWeekStats()
        loadLeaderboard()
        loadGameScore()
        viewModelScope.launch {
            dictionaryManager.currentVocabularyIdFlow
                .filterNotNull()
                .distinctUntilChanged()
                .collect { catId ->

                    loadWeekStats()
                }
        }

    }

    private fun loadGameScore() {
        viewModelScope.launch {
            try {
                val userId = tokenManager.getUserId()?.toInt() ?: return@launch
                val totalScore = ServiceLocator.gameResultDao.getUserTotalScore(userId)
                _yourGameScore.value = totalScore

            } catch (e: Exception) {

            }
        }
    }

    fun loadWeekStats() {
        viewModelScope.launch {
            val userId = tokenManager.getUserId()?.toInt() ?: return@launch


            val stats = dictionaryManager.getLastWeekStats(userId)



            val hasData = stats.any { it.newWords > 0 || it.inProgressWords > 0 || it.learnedWords > 0 }
            if (!hasData) {
                val mockStats = generateMockWeekStats(_totalStats.value)
                _weekStats.value = mockStats

            } else {
                _weekStats.value = stats

            }
        }
    }
    
    private fun generateMockWeekStats(totalStats: TotalStats): List<DailyStats> {
        val dates = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


        for (i in 6 downTo 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            dates.add(sdf.format(calendar.time))
        }


        if (totalStats.learned == 0 && totalStats.inProgress == 0 && totalStats.new == 0) {
            return dates.mapIndexed { index, date ->
                DailyStats(
                    date = date,
                    newWords = (index + 1) * 2,
                    inProgressWords = index + 1,
                    learnedWords = index
                )
            }
        }


        return dates.mapIndexed { index, date ->
            val learned = totalStats.learned / 7
            val inProgress = totalStats.inProgress / 7
            val new = totalStats.new / 7

            DailyStats(
                date = date,
                newWords = new.coerceAtLeast(1),
                inProgressWords = inProgress.coerceAtLeast(1),
                learnedWords = learned
            )
        }
    }

    fun loadServerStats() {
        viewModelScope.launch {

            try {
                val accessToken = tokenManager.getAccessToken().firstOrNull()

                if (accessToken != null) {
                    val response = wordApi.getStats("Bearer $accessToken")

                    if (response.isSuccessful && response.body() != null) {
                        val stats = response.body()!!
                        val newTotalStats = TotalStats(
                            new = stats.newWords,
                            inProgress = stats.inProgressWords,
                            learned = stats.learnedWords
                        )
                        _totalStats.value = newTotalStats



                        loadWeekStatsWithStats(newTotalStats)
                        return@launch
                    }
                }
            } catch (e: Exception) {

                _totalStats.value = TotalStats(0, 0, 0)
            }

            loadWeekStats()
        }
    }

    private fun loadWeekStatsWithStats(totalStats: TotalStats) {
        viewModelScope.launch {
            val userId = tokenManager.getUserId()?.toInt() ?: return@launch



            val stats = dictionaryManager.getLastWeekStats(userId)



            val hasData = stats.any { it.newWords > 0 || it.inProgressWords > 0 || it.learnedWords > 0 }
            if (!hasData) {
                val mockStats = generateMockWeekStats(totalStats)
                _weekStats.value = mockStats

            } else {
                _weekStats.value = stats

            }
        }
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            leaderboardManager.loadLeaderboard(currentSeasonId)
            _leaderboardPlayers.value = leaderboardManager.players.value
            _yourPosition.value = leaderboardManager.yourPosition.value
            _seasonText.value = leaderboardManager.seasonText.value

            viewModelScope.launch {
                leaderboardManager.players.collect { players ->
                    _leaderboardPlayers.value = players
                }
            }
            viewModelScope.launch {
                leaderboardManager.yourPosition.collect { pos ->
                    _yourPosition.value = pos
                }
            }
        }
    }


    data class TotalStats(val new: Int, val inProgress: Int, val learned: Int)
}
