package com.example.t_learnappmobile.presentation.statistics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.local.GameLocalSource
import com.example.t_learnappmobile.data.sync.SyncManager
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
import kotlinx.coroutines.withTimeoutOrNull

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
    val currentWeekOffset: Int = 0,
    val pendingSyncCount: Int = 0,
    val isOffline: Boolean = false
)

class StatisticsViewModel(
    private val getWordStatsUseCase: GetWordStatsUseCase,
    private val getWeeklyStatsUseCase: GetWeeklyStatsUseCase,
    private val getLeaderboardUseCase: GetLeaderboardUseCase,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val settingsUseCase: SettingsUseCase,
    private val syncManager: SyncManager,
    private val gameLocalSource: GameLocalSource
) : ViewModel() {

    companion object {
        private const val TAG = "StatisticsVM"
        private const val TIMEOUT_MS = 5000L
    }

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var userId: String? = null
    private var cachedLeaderboard: List<LeaderboardPlayer>? = null
    private var cachedWeeklyStats: List<DailyStats>? = null

    fun refreshStats(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isOffline = false)

            try {
                if (forceRefresh) {
                    clearCache()
                }

                syncManager.syncPendingGames()
                syncManager.syncPendingChanges()

                userId = authRepository.getCurrentUserId()
                if (userId == null) {
                    Log.e(TAG, "User not authenticated")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }

                val profile = withTimeoutOrNull(TIMEOUT_MS) {
                    userRepository.getUserProfile(userId!!)
                }

                val dictName = settingsUseCase.getCurrentDictionaryName() ?: "Все словари"
                val dictId = settingsUseCase.getCurrentDictionaryId() ?: "finance"

                _uiState.value = _uiState.value.copy(
                    firstName = profile?.firstName ?: "",
                    lastName = profile?.lastName ?: "",
                    yourUserId = userId!!,
                    yourGameScore = profile?.totalScore ?: gameLocalSource.getLocalUserScore(userId!!),
                    dictionaryName = dictName
                )

                loadWordStats(userId!!, dictId)
                loadWeeklyStatsWithTimeout(userId!!, _uiState.value.currentWeekOffset)
                loadLeaderboardWithTimeout(userId!!)

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing stats", e)
                _uiState.value = _uiState.value.copy(isLoading = false, isOffline = true)
            }
        }
    }

    fun forceRefresh() {
        clearCache()
        _uiState.value = StatisticsUiState(currentWeekOffset = 0)
        refreshStats(true)
    }

    fun clearCache() {
        Log.d(TAG, "Clearing all cache")
        cachedLeaderboard = null
        cachedWeeklyStats = null
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
            Log.e(TAG, "Error loading word stats", e)
        }
    }

    private suspend fun loadWeeklyStatsWithTimeout(userId: String, weekOffset: Int) {
        try {
            val weeklyStats = withTimeoutOrNull(TIMEOUT_MS) {
                getWeeklyStatsUseCase(userId, weekOffset)
            }

            if (weeklyStats != null) {
                cachedWeeklyStats = weeklyStats
                updateWeeklyStatsUI(weeklyStats)
                Log.d(TAG, "Weekly stats loaded: ${weeklyStats.sumOf { it.totalScore }} points")
            } else if (cachedWeeklyStats != null) {
                updateWeeklyStatsUI(cachedWeeklyStats!!)
                Log.d(TAG, "Using cached weekly stats")
            } else {
                updateWeeklyStatsUI(emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading weekly stats", e)
            if (cachedWeeklyStats != null) {
                updateWeeklyStatsUI(cachedWeeklyStats!!)
            } else {
                updateWeeklyStatsUI(emptyList())
            }
        }
    }

    private fun updateWeeklyStatsUI(weeklyStats: List<DailyStats>) {
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
    }

    private suspend fun loadLeaderboardWithTimeout(userId: String) {
        try {
            val leaderboard = withTimeoutOrNull(TIMEOUT_MS) {
                getLeaderboardUseCase(100)
            }

            if (leaderboard != null) {
                cachedLeaderboard = leaderboard
                updateLeaderboardUI(leaderboard, userId)
                Log.d(TAG, "Leaderboard loaded: ${leaderboard.size} players")
            } else if (cachedLeaderboard != null) {
                updateLeaderboardUI(cachedLeaderboard!!, userId)
                Log.d(TAG, "Using cached leaderboard")
            } else {
                val localScore = gameLocalSource.getLocalUserScore(userId)
                val fakeLeaderboard = listOf(
                    LeaderboardPlayer(
                        id = userId,
                        name = "${_uiState.value.firstName} ${_uiState.value.lastName}".trim(),
                        score = localScore,
                        position = 1
                    )
                )
                updateLeaderboardUI(fakeLeaderboard, userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading leaderboard", e)
            if (cachedLeaderboard != null) {
                updateLeaderboardUI(cachedLeaderboard!!, userId)
            }
        }
    }

    private fun updateLeaderboardUI(leaderboard: List<LeaderboardPlayer>, userId: String) {
        val yourPosition = leaderboard.find { it.id == userId }
        _uiState.value = _uiState.value.copy(
            leaderboard = leaderboard,
            yourPosition = yourPosition,
            yourGameScore = yourPosition?.score ?: _uiState.value.yourGameScore
        )
    }

    fun previousWeek() {
        val newOffset = _uiState.value.currentWeekOffset - 1
        _uiState.value = _uiState.value.copy(currentWeekOffset = newOffset)
        viewModelScope.launch {
            loadWeeklyStatsWithTimeout(userId ?: return@launch, newOffset)
        }
    }

    fun nextWeek() {
        if (_uiState.value.currentWeekOffset < 0) {
            val newOffset = _uiState.value.currentWeekOffset + 1
            _uiState.value = _uiState.value.copy(currentWeekOffset = newOffset)
            viewModelScope.launch {
                loadWeeklyStatsWithTimeout(userId ?: return@launch, newOffset)
            }
        }
    }

    fun onUserChanged() {
        clearCache()
        _uiState.value = StatisticsUiState(currentWeekOffset = 0)
        refreshStats(true)
    }
}