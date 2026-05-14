// data/remote/FirebaseGameSource.kt
package com.example.t_learnappmobile.data.remote

import android.content.Context
import android.util.Log
import com.example.t_learnappmobile.domain.model.DailyStats
import com.example.t_learnappmobile.domain.model.GameResult
import com.example.t_learnappmobile.domain.model.GameWord
import com.example.t_learnappmobile.domain.model.LeaderboardPlayer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// data/remote/FirebaseGameSource.kt
class FirebaseGameSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: Context
) {
    // ...

    private val TAG = "GameSource"

    suspend fun loadGameWords(dictionaryId: String, limit: Int): List<GameWord> {
        return try {
            firestore.collection("words")
                .whereEqualTo("dictionaryId", dictionaryId)
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    GameWord(
                        id = doc.id.hashCode().toLong(),
                        english = data["englishWord"] as? String ?: "",
                        russian = data["translation"] as? String ?: ""
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading game words", e)
            emptyList()
        }
    }

    suspend fun saveGameResult(userId: String, score: Int, totalWords: Int) {
        try {
            val gameResult = mapOf(
                "userId" to userId,
                "score" to score,
                "wordsCount" to totalWords,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("game_results")
                .add(gameResult)
                .await()

            Log.d(TAG, "Game result saved: score=$score for user=$userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving game result", e)
            saveGameResultLocally(userId, score, totalWords)
        }
    }

    suspend fun getGameResults(userId: String): List<GameResult> {
        return try {
            firestore.collection("game_results")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    GameResult(
                        score = (data["score"] as? Long)?.toInt() ?: 0,
                        totalWords = (data["wordsCount"] as? Long)?.toInt() ?: 0,
                        timestamp = data["timestamp"] as? Long ?: 0
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting game results", e)
            emptyList()
        }
    }

    suspend fun getWeeklyStats(userId: String, weekOffset: Int): List<DailyStats> {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
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
                val score = (doc.getLong("score")?.toInt() ?: 0)
                val date = dateFormat.format(Date(timestamp))
                val current = statsMap[date] ?: continue
                statsMap[date] = current.copy(
                    gamesPlayed = current.gamesPlayed + 1,
                    totalScore = current.totalScore + score
                )
            }

            statsMap.values.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting weekly stats", e)
            emptyList()
        }
    }

    suspend fun getLeaderboard(limit: Int = 100): List<LeaderboardPlayer> {
        return try {
            firestore.collection("leaderboard")
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .documents
                .mapIndexed { index, doc ->
                    LeaderboardPlayer(
                        id = doc.id,
                        name = doc.getString("username") ?: "Player",
                        score = (doc.getLong("totalScore")?.toInt() ?: 0),
                        position = index + 1
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting leaderboard", e)
            emptyList()
        }
    }

    suspend fun getPlayerPosition(userId: String): LeaderboardPlayer? {
        return try {
            val userDoc = firestore.collection("leaderboard").document(userId).get().await()
            if (userDoc.exists()) {
                val userData = userDoc.data
                val userScore = userData?.get("totalScore") as? Long ?: return null

                val higherScoresSnapshot = firestore.collection("leaderboard")
                    .whereGreaterThan("totalScore", userScore)
                    .get()
                    .await()

                val position = higherScoresSnapshot.documents.size + 1

                LeaderboardPlayer(
                    id = userId,
                    name = userData["username"] as? String ?: "Player",
                    score = userScore.toInt(),
                    position = position
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting player position", e)
            null
        }
    }

    private fun saveGameResultLocally(userId: String, score: Int, wordsCount: Int) {
        try {
            val prefs = context.getSharedPreferences("pending_game_results", Context.MODE_PRIVATE)
            val pendingResults = prefs.getStringSet("pending_results", mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()

            val resultJson = "$userId|$score|$wordsCount|${System.currentTimeMillis()}"
            pendingResults.add(resultJson)

            prefs.edit().putStringSet("pending_results", pendingResults).apply()
            Log.d(TAG, "Game result saved locally for later sync")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving locally", e)
        }
    }
}