package com.example.t_learnappmobile.presentation.statistics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.firebase.DailyStats
import com.example.t_learnappmobile.data.leaderboard.LeaderboardPlayer
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.settings.SettingsManager
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

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

class StatisticsViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val authManager = ServiceLocator.firebaseAuthManager
    private val userRepository = ServiceLocator.userRepository
    private val settingsManager = ServiceLocator.appContext?.let { SettingsManager(it) }

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    fun refreshStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val userId = authManager.getUserId()

                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,

                    )
                    return@launch
                }

                val profile = userRepository.getCurrentUserProfile()
                _uiState.value = _uiState.value.copy(
                    firstName = profile?.firstName ?: "",
                    lastName = profile?.lastName ?: "",
                    yourUserId = userId,
                    yourGameScore = profile?.totalScore ?: 0
                )

                val dictName = settingsManager?.getCurrentDictionaryName() ?: "Все словари"
                val dictId = settingsManager?.getCurrentCategoryId() ?: "finance"
                _uiState.value = _uiState.value.copy(dictionaryName = dictName)

                loadWordStats(userId, dictId)
                loadGameStats(userId)
                loadWeeklyStats(userId, _uiState.value.currentWeekOffset)
                loadLeaderboard(userId)

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                Log.e("StatisticsVM", "Error refreshing stats", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,

                )
            }
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

    private suspend fun loadWordStats(userId: String, dictionaryId: String) {
        try {
            val userWordsSnapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()

            var newWords = 0
            var inProgress = 0
            var learned = 0

            for (doc in userWordsSnapshot.documents) {
                val stage = (doc.getLong("stage") ?: 0).toInt()
                when {
                    stage == 0 -> newWords++
                    stage in 1..6 -> inProgress++
                    stage >= 7 -> learned++
                }
            }

            _uiState.value = _uiState.value.copy(
                newWords = newWords,
                inProgressWords = inProgress,
                learnedWords = learned
            )
        } catch (e: Exception) {
            Log.e("StatisticsVM", "Error loading word stats", e)
        }
    }

    private suspend fun loadGameStats(userId: String) {
        try {
            val gamesSnapshot = firestore.collection("game_results")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            var totalGames = 0
            var totalScore = 0

            for (doc in gamesSnapshot.documents) {
                totalGames++
                totalScore += (doc.getLong("score") ?: 0).toInt()
            }

            _uiState.value = _uiState.value.copy(
                totalGamesPlayed = totalGames,
                totalScore = totalScore,
                averageScore = if (totalGames > 0) totalScore / totalGames else 0
            )
        } catch (e: Exception) {
            Log.e("StatisticsVM", "Error loading game stats", e)
        }
    }

    private suspend fun loadWeeklyStats(userId: String, weekOffset: Int) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("ru"))

            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Находим понедельник текущей недели
                val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                val daysToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
                add(Calendar.DAY_OF_YEAR, daysToMonday)
                add(Calendar.WEEK_OF_YEAR, weekOffset)
            }

            val endCal = startCal.clone() as Calendar
            endCal.add(Calendar.DAY_OF_YEAR, 7)

            val startTime = startCal.timeInMillis
            val endTime = endCal.timeInMillis

            val snapshot = firestore.collection("game_results")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .whereLessThan("timestamp", endTime)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            val statsMap = linkedMapOf<String, DailyStats>()
            for (i in 0 until 7) {
                val dayCal = startCal.clone() as Calendar
                dayCal.add(Calendar.DAY_OF_YEAR, i)
                val date = dateFormat.format(dayCal.time)
                statsMap[date] = DailyStats(date = date)
            }

            for (doc in snapshot.documents) {
                val timestamp = doc.getLong("timestamp") ?: continue
                val score = doc.getLong("score")?.toInt() ?: 0
                val date = dateFormat.format(Date(timestamp))
                val current = statsMap[date] ?: continue
                statsMap[date] = current.copy(
                    gamesPlayed = current.gamesPlayed + 1,
                    totalScore = current.totalScore + score
                )
            }

            _uiState.value = _uiState.value.copy(weeklyStats = statsMap.values.toList())
        } catch (e: Exception) {
            Log.e("StatisticsVM", "Error loading weekly stats", e)
        }
    }

    private suspend fun loadLeaderboard(currentUserId: String) {
        try {
            val snapshot = firestore.collection("leaderboard")
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()

            val players = snapshot.documents.mapIndexed { index, doc ->
                LeaderboardPlayer(
                    id = doc.id,
                    name = doc.getString("username") ?: "Игрок",
                    score = doc.getLong("totalScore")?.toInt() ?: 0,
                    position = index + 1
                )
            }

            var yourPosition = players.find { it.id == currentUserId }

            if (yourPosition == null) {
                val userDoc = firestore.collection("leaderboard")
                    .document(currentUserId)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    val userScore = userDoc.getLong("totalScore")?.toInt() ?: 0
                    val userName = userDoc.getString("username") ?: "Вы"
                    yourPosition = LeaderboardPlayer(
                        id = currentUserId,
                        name = userName,
                        score = userScore,
                        position = players.size + 1
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                leaderboard = players,
                yourPosition = yourPosition,
                yourGameScore = yourPosition?.score ?: _uiState.value.yourGameScore
            )
        } catch (e: Exception) {
            Log.e("StatisticsVM", "Error loading leaderboard", e)
        }
    }
}