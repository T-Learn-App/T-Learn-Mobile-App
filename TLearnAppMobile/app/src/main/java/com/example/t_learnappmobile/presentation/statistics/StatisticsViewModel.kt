package com.example.t_learnappmobile.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.leaderboard.LeaderboardPlayer
import com.example.t_learnappmobile.data.repository.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class StatisticsViewModel : ViewModel() {
    private val firestore = ServiceLocator.firestore
    private val authManager = ServiceLocator.firebaseAuthManager
    private val userRepository = ServiceLocator.userRepository

    data class DailyStats(val date: String, val inProgressWords: Int = 0, val learnedWords: Int = 0)
    data class TotalStats(val new: Int = 0, val inProgress: Int = 0, val learned: Int = 0)

    private val _weekStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val weekStats: StateFlow<List<DailyStats>> = _weekStats
    private val _totalStats = MutableStateFlow(TotalStats())
    val totalStats: StateFlow<TotalStats> = _totalStats
    private val _leaderboardPlayers = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val leaderboardPlayers: StateFlow<List<LeaderboardPlayer>> = _leaderboardPlayers
    private val _yourPosition = MutableStateFlow<LeaderboardPlayer?>(null)
    val yourPosition: StateFlow<LeaderboardPlayer?> = _yourPosition
    private val _yourGameScore = MutableStateFlow(0)
    val yourGameScore: StateFlow<Int> = _yourGameScore
    val currentDictionaryName = MutableStateFlow("Все слова")
    val isLeaderboardLoading = MutableStateFlow(false)

    init {
        loadLeaderboard()
        loadUserScore()
        generateWeekStats()
    }

    fun refreshStats() {
        loadLeaderboard()
        loadUserScore()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            try {
                isLeaderboardLoading.value = true
                val snapshot = firestore.collection("leaderboard")
                    .orderBy("totalScore", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(100)
                    .get()
                    .await()

                val players = snapshot.documents.mapIndexed { index, doc ->
                    LeaderboardPlayer(
                        id = doc.id.hashCode(),
                        name = doc.getString("username") ?: "Anonymous",
                        score = doc.getLong("totalScore")?.toInt() ?: 0,
                        position = index + 1
                    )
                }
                _leaderboardPlayers.value = players

                val userId = authManager.getUserId()
                _yourPosition.value = players.find { it.id.toString() == userId }
            } catch (e: Exception) {
                // ignore
            } finally {
                isLeaderboardLoading.value = false
            }
        }
    }

    private fun loadUserScore() {
        viewModelScope.launch {
            val profile = userRepository.getCurrentUserProfile()
            _yourGameScore.value = profile?.totalScore ?: 0
        }
    }

    private fun generateWeekStats() {
        val weekStats = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        repeat(7) {
            weekStats.add(DailyStats(date = dateFormat.format(calendar.time)))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        _weekStats.value = weekStats
    }
}