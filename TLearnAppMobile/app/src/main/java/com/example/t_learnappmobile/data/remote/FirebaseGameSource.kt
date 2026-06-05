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
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.*

class FirebaseGameSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: Context
) {

    private val TAG = "GameSource"

    companion object {
        private const val PREFS_NAME = "game_cache"
        private const val KEY_PENDING_GAME_RESULTS = "pending_game_results"
        private const val KEY_USER_SCORE = "user_score_"
    }

    suspend fun getWeeklyStats(userId: String, weekOffset: Int): List<DailyStats> {
        Log.d(TAG, "=== getWeeklyStats called ===")
        Log.d(TAG, "userId: $userId, weekOffset: $weekOffset")

        return try {
            withTimeoutOrNull(5000L) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                val startCal = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)

                    val currentDayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val daysToMonday = if (currentDayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - currentDayOfWeek
                    add(Calendar.DAY_OF_YEAR, daysToMonday)

                    add(Calendar.WEEK_OF_YEAR, weekOffset)
                }

                val endCal = startCal.clone() as Calendar
                endCal.add(Calendar.DAY_OF_YEAR, 7)

                val startTime = startCal.timeInMillis
                val endTime = endCal.timeInMillis

                Log.d(TAG, "Week range: ${Date(startTime)} to ${Date(endTime)}")

                val snapshot = firestore.collection("game_results")
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("timestamp", startTime)
                    .whereLessThan("timestamp", endTime)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
                    .await()

                Log.d(TAG, "Found ${snapshot.documents.size} game results for this week")

                val statsMap = linkedMapOf<String, DailyStats>()
                for (i in 0 until 7) {
                    val dayCal = startCal.clone() as Calendar
                    dayCal.add(Calendar.DAY_OF_YEAR, i)
                    val date = dateFormat.format(dayCal.time)
                    statsMap[date] = DailyStats(date = date, gamesPlayed = 0, totalScore = 0)
                }

                for (doc in snapshot.documents) {
                    val timestamp = doc.getLong("timestamp") ?: continue
                    val score = (doc.getLong("score")?.toInt() ?: 0)
                    val date = dateFormat.format(Date(timestamp))

                    val current = statsMap[date]
                    if (current != null) {
                        statsMap[date] = current.copy(
                            gamesPlayed = current.gamesPlayed + 1,
                            totalScore = current.totalScore + score
                        )
                        Log.d(TAG, "Added game on $date: score=$score, total games now=${current.gamesPlayed + 1}")
                    }
                }

                val result = statsMap.values.toList()
                Log.d(TAG, "Weekly stats result: $result")
                result
            } ?: run {
                Log.e(TAG, "Timeout getting weekly stats")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting weekly stats", e)
            emptyList()
        }
    }

    suspend fun getLeaderboard(limit: Int): List<LeaderboardPlayer> {
        return try {
            withTimeoutOrNull(5000L) {
                val snapshot = firestore.collection("leaderboard")
                    .orderBy("totalScore", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()

                var position = 1
                snapshot.documents.mapNotNull { doc ->
                    val userId = doc.id
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    if (!userDoc.exists()) return@mapNotNull null

                    LeaderboardPlayer(
                        id = userId,
                        name = doc.getString("username") ?: "Player",
                        score = (doc.getLong("totalScore")?.toInt() ?: 0),
                        position = position++
                    )
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting leaderboard", e)
            emptyList()
        }
    }

    suspend fun loadGameWords(dictionaryId: String, limit: Int): List<GameWord> {
        return try {
            val allWords = firestore.collection("words")
                .whereEqualTo("dictionaryId", dictionaryId)
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

            val shuffledWords = allWords.shuffled()
            val result = shuffledWords.take(limit)

            Log.d(TAG, "Loaded ${result.size} words for game from dictionary: $dictionaryId")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error loading game words", e)
            emptyList()
        }
    }

    suspend fun saveGameResult(userId: String, score: Int, totalWords: Int) {
        try {
            val timestamp = System.currentTimeMillis()
            val gameResult = mapOf(
                "userId" to userId,
                "score" to score,
                "wordsCount" to totalWords,
                "timestamp" to timestamp
            )

            firestore.collection("game_results")
                .add(gameResult)
                .await()

            Log.d(TAG, "Game result saved: userId=$userId, score=$score, timestamp=$timestamp, date=${Date(timestamp)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving game result, saving locally", e)
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

    suspend fun getPlayerPosition(userId: String): LeaderboardPlayer? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            if (!userDoc.exists()) {
                Log.d(TAG, "User $userId does not exist")
                return null
            }

            val leaderboardDoc = firestore.collection("leaderboard").document(userId).get().await()
            if (!leaderboardDoc.exists()) {
                val firstName = userDoc.getString("firstName") ?: ""
                val lastName = userDoc.getString("lastName") ?: ""
                val username = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                    "$firstName $lastName".trim()
                } else {
                    userDoc.getString("email")?.split("@")?.firstOrNull() ?: "User"
                }

                firestore.collection("leaderboard").document(userId).set(
                    mapOf(
                        "userId" to userId,
                        "username" to username,
                        "totalScore" to 0,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()

                return LeaderboardPlayer(
                    id = userId,
                    name = username,
                    score = 0,
                    position = getPositionForScore(userId, 0)
                )
            }

            val userData = leaderboardDoc.data
            val userScore = userData?.get("totalScore") as? Long ?: 0
            val username = userData?.get("username") as? String ?: "Player"

            val higherScoresSnapshot = firestore.collection("leaderboard")
                .whereGreaterThan("totalScore", userScore)
                .get()
                .await()

            val position = higherScoresSnapshot.documents.size + 1

            LeaderboardPlayer(
                id = userId,
                name = username,
                score = userScore.toInt(),
                position = position
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting player position", e)
            null
        }
    }

    private suspend fun getPositionForScore(userId: String, score: Int): Int {
        return try {
            val higherScores = firestore.collection("leaderboard")
                .whereGreaterThan("totalScore", score)
                .get()
                .await()
            higherScores.documents.size + 1
        } catch (e: Exception) {
            999
        }
    }

    private fun saveGameResultLocally(userId: String, score: Int, wordsCount: Int) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val pendingResultsJson = prefs.getString(KEY_PENDING_GAME_RESULTS, null)
            val pendingResults = mutableListOf<PendingGameResult>()

            if (!pendingResultsJson.isNullOrEmpty()) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<PendingGameResult>>() {}.type
                    val existing = com.google.gson.Gson().fromJson<List<PendingGameResult>>(pendingResultsJson, type)
                    if (existing != null) {
                        pendingResults.addAll(existing)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing pending results", e)
                }
            }

            pendingResults.add(
                PendingGameResult(
                    userId = userId,
                    score = score,
                    wordsCount = wordsCount,
                    timestamp = System.currentTimeMillis()
                )
            )

            val json = com.google.gson.Gson().toJson(pendingResults)
            prefs.edit().putString(KEY_PENDING_GAME_RESULTS, json).apply()

            updateLocalUserScore(userId, score)

            Log.d(TAG, "Game result saved locally: score=$score for user=$userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving game result locally", e)
        }
    }

    private fun updateLocalUserScore(userId: String, score: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_USER_SCORE + userId
        val currentScore = prefs.getInt(key, 0)
        prefs.edit().putInt(key, currentScore + score).apply()
        Log.d(TAG, "Local user score updated: +$score, total=${currentScore + score}")
    }

    suspend fun getPendingGameResults(): List<PendingGameResult> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_PENDING_GAME_RESULTS, null)
            if (json.isNullOrEmpty()) {
                return emptyList()
            }
            val type = object : com.google.gson.reflect.TypeToken<List<PendingGameResult>>() {}.type
            com.google.gson.Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pending game results", e)
            emptyList()
        }
    }
}

data class PendingGameResult(
    val userId: String,
    val score: Int,
    val wordsCount: Int,
    val timestamp: Long
)