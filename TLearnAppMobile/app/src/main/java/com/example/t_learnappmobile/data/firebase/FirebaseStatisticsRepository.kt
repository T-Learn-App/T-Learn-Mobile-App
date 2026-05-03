package com.example.t_learnappmobile.data.firebase

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class DailyStats(
    val date: String,
    val learnedWords: Int = 0,
    val gamesPlayed: Int = 0,
    val totalScore: Int = 0
)

data class TotalStats(
    val totalWordsLearned: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalScore: Int = 0,
    val averageScore: Int = 0
)

class FirebaseStatisticsRepository {
    private val firestore = Firebase.firestore
    private val auth = ServiceLocator.firebaseAuthManager
    private val TAG = "FirebaseStatsRepo"

    private val _dailyStats = MutableStateFlow<List<DailyStats>>(emptyList())
    val dailyStats: StateFlow<List<DailyStats>> = _dailyStats.asStateFlow()

    private val _totalStats = MutableStateFlow(TotalStats())
    val totalStats: StateFlow<TotalStats> = _totalStats.asStateFlow()

    suspend fun loadStatistics() {
        val userId = auth.getUserId() ?: return
        try {
            // Загружаем результаты игр за последние 7 дней
            val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            val gamesSnapshot = firestore.collection("game_results")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("timestamp", sevenDaysAgo)
                .get()
                .await()

            // Загружаем прогресс слов
            val wordsSnapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // Подсчитываем выученные слова (stage >= 7)
            val learnedWords = wordsSnapshot.documents.count { doc ->
                (doc.getLong("stage") ?: 0) >= 7
            }

            // Группируем игры по дням
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val statsMap = mutableMapOf<String, DailyStats>()

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
                val wordsCount = doc.getLong("wordsCount")?.toInt() ?: 0

                totalGamesPlayed++
                totalScoreSum += score

                val existing = statsMap[date] ?: DailyStats(date = date)
                statsMap[date] = existing.copy(
                    gamesPlayed = existing.gamesPlayed + 1,
                    totalScore = existing.totalScore + score
                )
            }

            _dailyStats.value = statsMap.values.sortedBy { it.date }
            _totalStats.value = TotalStats(
                totalWordsLearned = learnedWords,
                totalGamesPlayed = totalGamesPlayed,
                totalScore = totalScoreSum,
                averageScore = if (totalGamesPlayed > 0) totalScoreSum / totalGamesPlayed else 0
            )

            Log.d(TAG, "Statistics loaded: learned=$learnedWords, games=$totalGamesPlayed")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading statistics", e)
        }
    }
}