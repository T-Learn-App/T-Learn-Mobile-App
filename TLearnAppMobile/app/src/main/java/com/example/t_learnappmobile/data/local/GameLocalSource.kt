package com.example.t_learnappmobile.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameLocalSource(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("game_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PENDING_GAME_RESULTS = "pending_game_results"
        private const val KEY_CACHED_LEADERBOARD = "cached_leaderboard"
        private const val KEY_CACHED_LEADERBOARD_TIME = "cached_leaderboard_time"
        private const val KEY_USER_SCORE = "user_score_"
        private const val CACHE_DURATION_MS = 300_000L
    }

    suspend fun savePendingGameResult(userId: String, score: Int, wordsCount: Int) {
        withContext(Dispatchers.IO) {
            try {
                val pendingResults = getPendingGameResults().toMutableList()
                val result = PendingGameResult(
                    userId = userId,
                    score = score,
                    wordsCount = wordsCount,
                    timestamp = System.currentTimeMillis()
                )
                pendingResults.add(result)

                val json = gson.toJson(pendingResults)
                prefs.edit().putString(KEY_PENDING_GAME_RESULTS, json).apply()

                updateLocalUserScore(userId, score)

                Log.d("GameLocalSource", "Saved pending game result: score=$score, total pending=${pendingResults.size}")
            } catch (e: Exception) {
                Log.e("GameLocalSource", "Error saving pending game result", e)
            }
        }
    }

    suspend fun getPendingGameResults(): List<PendingGameResult> {
        return withContext(Dispatchers.IO) {
            try {
                val json = prefs.getString(KEY_PENDING_GAME_RESULTS, null)
                if (json.isNullOrEmpty()) {
                    return@withContext emptyList()
                }
                val type = object : TypeToken<List<PendingGameResult>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                Log.e("GameLocalSource", "Error getting pending game results", e)
                emptyList()
            }
        }
    }

    suspend fun removePendingGameResults(results: List<PendingGameResult>) {
        withContext(Dispatchers.IO) {
            try {
                val currentResults = getPendingGameResults().toMutableList()
                currentResults.removeAll(results)
                val json = gson.toJson(currentResults)
                prefs.edit().putString(KEY_PENDING_GAME_RESULTS, json).apply()
                Log.d("GameLocalSource", "Removed ${results.size} synced game results")
            } catch (e: Exception) {
                Log.e("GameLocalSource", "Error removing pending game results", e)
            }
        }
    }

    fun updateLocalUserScore(userId: String, score: Int) {
        val key = KEY_USER_SCORE + userId
        val currentScore = prefs.getInt(key, 0)
        val newScore = currentScore + score
        prefs.edit().putInt(key, newScore).apply()
        Log.d("GameLocalSource", "Local user score updated: $currentScore -> $newScore (+$score)")
    }

    fun getLocalUserScore(userId: String): Int {
        val key = KEY_USER_SCORE + userId
        return prefs.getInt(key, 0)
    }

    fun clearUserScore(userId: String) {
        val key = KEY_USER_SCORE + userId
        prefs.edit().remove(key).apply()
        Log.d("GameLocalSource", "Cleared user score for: $userId")
    }

    suspend fun cacheLeaderboard(leaderboard: List<LeaderboardCacheEntry>) {
        withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(leaderboard)
                prefs.edit()
                    .putString(KEY_CACHED_LEADERBOARD, json)
                    .putLong(KEY_CACHED_LEADERBOARD_TIME, System.currentTimeMillis())
                    .apply()
                Log.d("GameLocalSource", "Cached leaderboard with ${leaderboard.size} entries")
            } catch (e: Exception) {
                Log.e("GameLocalSource", "Error caching leaderboard", e)
            }
        }
    }

    suspend fun getCachedLeaderboard(): List<LeaderboardCacheEntry>? {
        return withContext(Dispatchers.IO) {
            try {
                val lastUpdate = prefs.getLong(KEY_CACHED_LEADERBOARD_TIME, 0)
                val isExpired = System.currentTimeMillis() - lastUpdate > CACHE_DURATION_MS

                if (isExpired) {
                    return@withContext null
                }

                val json = prefs.getString(KEY_CACHED_LEADERBOARD, null)
                if (json.isNullOrEmpty()) {
                    return@withContext null
                }
                val type = object : TypeToken<List<LeaderboardCacheEntry>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                Log.e("GameLocalSource", "Error getting cached leaderboard", e)
                null
            }
        }
    }

    fun clearAllUserData() {
        prefs.edit().clear().apply()
        Log.d("GameLocalSource", "Cleared all game cache")
    }
}

data class PendingGameResult(
    val userId: String,
    val score: Int,
    val wordsCount: Int,
    val timestamp: Long
)

data class LeaderboardCacheEntry(
    val id: String,
    val name: String,
    val score: Int,
    val position: Int
)