// presentation/screens/statistics/StatisticsViewModel.kt
package com.example.t_learnappmobile.presentation.statistics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.domain.model.DailyStats
import com.example.t_learnappmobile.domain.model.LeaderboardPlayer
import com.example.t_learnappmobile.domain.repository.AuthRepository
import com.example.t_learnappmobile.domain.repository.UserRepository
import com.example.t_learnappmobile.domain.usecase.game.GetLeaderboardUseCase
import com.example.t_learnappmobile.domain.usecase.game.GetWeeklyStatsUseCase
import com.example.t_learnappmobile.domain.usecase.settings.SettingsUseCase
import com.example.t_learnappmobile.domain.usecase.words.GetWordStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val isLoading: Boolean = false,
    val dictionaryName: String = "",
    val newWords: Int = 0,
    val inProgressWords: Int = 0,
    val learnedWords: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalScore: Int = 0,
    val averageScore: Int = 0,
    val weeklyStats: List<DailyStats> = emptyList(),
    val leaderboard: List<LeaderboardPlayer> = emptyList(),
    val yourPosition: LeaderboardPlayer? = null,
    val yourGameScore: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val yourUserId: String = "",
    val currentWeekOffset: Int = 0
)

class StatisticsViewModel(
    private val getWordStatsUseCase: GetWordStatsUseCase,
    private val getWeeklyStatsUseCase: GetWeeklyStatsUseCase,
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var userId: String? = null

    fun refreshStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }

                val profile = userRepository.getUserProfile(userId!!)
                val dictName = settingsUseCase.getCurrentDictionaryName() ?: "All Dictionaries"
                val dictId = settingsUseCase.getCurrentDictionaryId() ?: "finance"

                _uiState.value = _uiState.value.copy(
                    firstName = profile?.firstName ?: "",
                    lastName = profile?.lastName ?: "",
                    yourUserId = userId!!,
                    yourGameScore = profile?.totalScore ?: 0,
                    dictionaryName = dictName
                )

                // Load all stats in parallel
                loadWordStats(userId!!, dictId)
                loadWeeklyStats(userId!!, _uiState.value.currentWeekOffset)
                loadLeaderboard(userId!!)

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e("StatisticsVM", "Error refreshing stats", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun loadWordStats(userId: String, dictionaryId: String) {
        try {
            val stats = getWordStatsUseCase(userId, dictionaryId)
            _uiState.value = _uiState.value.copy(
                newWords = stats.newWords,
                inProgressWords = stats.inProgressWords,
                learnedWords = stats.learnedWords
            )
        } catch (e: Exception) {
            Log.e("StatisticsVM", "Error loading word stats", e)
        }
    }

    private suspend fun loadWeeklyStats(userId: String, weekOffset: Int) {
        try {
            val weeklyStats = getWeeklyStatsUseCase(userId, weekOffset)
            var totalGames = 0
            var totalScore = 0

            for (stat in weeklyStats) {
                totalGames += stat.gamesPlayed
                totalScore += stat.totalScore
            }

            _uiState.value = _uiState.value.copy(
                weeklyStats = weeklyStats,
                totalGamesPlayed = totalGames,
                totalScore = totalScore,
                averageScore = if (totalGames > 0) totalScore / totalGames else 0
            )
        } catch (e: Exception) {
            Log.e("StatisticsVM", "Error loading weekly stats", e)
        }
    }

    private suspend fun loadLeaderboard(userId: String) {
        try {
            val leaderboard = getLeaderboardUseCase()
            val yourPosition = leaderboard.find { it.id == userId }

            _uiState.value = _uiState.value.copy(
                leaderboard = leaderboard,
                yourPosition = yourPosition,
                yourGameScore = yourPosition?.score ?: _uiState.value.yourGameScore
            )
        } catch (e: Exception) {
            Log.e("StatisticsVM", "Error loading leaderboard", e)
        }
    }

    fun previousWeek() {
        _uiState.value = _uiState.value.copy(currentWeekOffset = _uiState.value.currentWeekOffset - 1)
        refreshStats()
    }

    fun nextWeek() {
        if (_uiState.value.currentWeekOffset < 0) {
            _uiState.value = _uiState.value.copy(currentWeekOffset = _uiState.value.currentWeekOffset + 1)
            refreshStats()
        }
    }
}