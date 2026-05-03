package com.example.t_learnappmobile.presentation.statistics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.t_learnappmobile.data.firebase.DailyStats
import com.example.t_learnappmobile.data.firebase.TotalStats
import com.example.t_learnappmobile.data.leaderboard.LeaderboardPlayer
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.data.settings.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class StatisticsViewModel : ViewModel() {
    private val firestore = ServiceLocator.firestore
    private val authManager = ServiceLocator.firebaseAuthManager
    private val TAG = "StatisticsViewModel"

    private val _weekStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val weekStats: StateFlow<List<DailyStats>> = _weekStats.asStateFlow()

    private val _totalStats = MutableStateFlow(TotalStats())
    val totalStats: StateFlow<TotalStats> = _totalStats.asStateFlow()

    private val _leaderboardPlayers = MutableStateFlow<List<LeaderboardPlayer>>(emptyList())
    val leaderboardPlayers: StateFlow<List<LeaderboardPlayer>> = _leaderboardPlayers.asStateFlow()

    private val _yourPosition = MutableStateFlow<LeaderboardPlayer?>(null)
    val yourPosition: StateFlow<LeaderboardPlayer?> = _yourPosition.asStateFlow()

    private val _yourGameScore = MutableStateFlow(0)
    val yourGameScore: StateFlow<Int> = _yourGameScore.asStateFlow()

    private val _isLeaderboardLoading = MutableStateFlow(false)
    val isLeaderboardLoading: StateFlow<Boolean> = _isLeaderboardLoading.asStateFlow()

    private val _learnedWordsCount = MutableStateFlow(0)
    val learnedWordsCount: StateFlow<Int> = _learnedWordsCount.asStateFlow()

    private val _inProgressWordsCount = MutableStateFlow(0)
    val inProgressWordsCount: StateFlow<Int> = _inProgressWordsCount.asStateFlow()

    private val _newWordsCount = MutableStateFlow(0)
    val newWordsCount: StateFlow<Int> = _newWordsCount.asStateFlow()

    private val _currentDictionaryName = MutableStateFlow("Все слова")
    val currentDictionaryName: StateFlow<String> = _currentDictionaryName.asStateFlow()

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            loadStatistics()
            loadLeaderboard()
            loadUserScore()
        }
    }

    private suspend fun loadStatistics() {
        val userId = authManager.getUserId() ?: return
        val context = ServiceLocator.appContext
        val settingsManager = SettingsManager(context)
        val currentDictId = settingsManager.getCurrentCategoryId()
        val currentDictName = settingsManager.getCurrentDictionaryName()

        _currentDictionaryName.value = currentDictName ?: "Все слова"

        try {
            // Загружаем все слова пользователя для текущего словаря
            val wordsSnapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .whereEqualTo("dictionaryId", currentDictId)
                .get()
                .await()

            var learned = 0
            var inProgress = 0
            var newWords = 0

            for (doc in wordsSnapshot.documents) {
                val stage = (doc.getLong("stage") ?: 0).toInt()
                when {
                    stage >= 7 -> learned++
                    stage in 1..6 -> inProgress++
                    stage == 0 -> newWords++
                }
            }

            _learnedWordsCount.value = learned
            _inProgressWordsCount.value = inProgress
            _newWordsCount.value = newWords

            Log.d(TAG, "Stats for dict $currentDictId: learned=$learned, inProgress=$inProgress, new=$newWords")

            // Загружаем результаты игр за последние 7 дней
            val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            val gamesSnapshot = firestore.collection("game_results")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("timestamp", sevenDaysAgo)
                .get()
                .await()

            // Группируем по дням
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val statsMap = mutableMapOf<String, DailyStats>()

            // Инициализируем все 7 дней
            for (i in 0 until 7) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = dateFormat.format(calendar.time)
                statsMap[date] = DailyStats(date = date)
            }

            var totalGamesPlayed = 0
            var totalScoreSum = 0

            for (doc in gamesSnapshot.documents) {
                val timestamp = doc.getLong("timestamp") ?: continue
                val date = dateFormat.format(Date(timestamp))
                val score = doc.getLong("score")?.toInt() ?: 0

                totalGamesPlayed++
                totalScoreSum += score

                val existing = statsMap[date] ?: DailyStats(date = date)
                statsMap[date] = existing.copy(
                    gamesPlayed = existing.gamesPlayed + 1,
                    totalScore = existing.totalScore + score
                )
            }

            _weekStats.value = statsMap.values.sortedBy { it.date }
            _totalStats.value = TotalStats(
                totalWordsLearned = learned,
                totalGamesPlayed = totalGamesPlayed,
                totalScore = totalScoreSum,
                averageScore = if (totalGamesPlayed > 0) totalScoreSum / totalGamesPlayed else 0
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error loading statistics", e)
        }
    }

    private suspend fun loadLeaderboard() {
        try {
            _isLeaderboardLoading.value = true
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
            Log.e(TAG, "Error loading leaderboard", e)
        } finally {
            _isLeaderboardLoading.value = false
        }
    }

    private suspend fun loadUserScore() {
        val profile = ServiceLocator.userRepository.getCurrentUserProfile()
        _yourGameScore.value = profile?.totalScore ?: 0
    }
}