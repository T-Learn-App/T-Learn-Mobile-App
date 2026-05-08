package com.example.t_learnappmobile.data.firebase

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.model.GameWord
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseGameRepository {
    private val firestore = Firebase.firestore
    private val auth = ServiceLocator.firebaseAuthManager
    private val TAG = "FirebaseGameRepo"

    private val _gameWords = MutableStateFlow<List<GameWord>>(emptyList())
    val gameWords: StateFlow<List<GameWord>> = _gameWords.asStateFlow()

    suspend fun loadGameWords(dictionaryId: String, limit: Int = 10): List<GameWord> {
        return try {
            val snapshot = firestore.collection("words")
                .whereEqualTo("dictionaryId", dictionaryId)
                .limit(limit.toLong())
                .get()
                .await()

            val words = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                GameWord(
                    id = doc.id.hashCode().toLong(),
                    english = data["englishWord"] as? String ?: "",
                    russian = data["translation"] as? String ?: ""
                )
            }
            _gameWords.value = words
            Log.d(TAG, "Loaded ${words.size} game words")
            words
        } catch (e: Exception) {
            Log.e(TAG, "Error loading game words", e)
            emptyList()
        }
    }

    suspend fun saveGameResult(score: Int, wordsCount: Int) {
        val userId = auth.getUserId()
        if (userId == null) {
            Log.e(TAG, "Cannot save game result: user not authenticated")
            return
        }

        try {
            // ✅ Сохраняем результат с таймаутом, чтобы не блокировать игру
            val gameResult = mapOf(
                "userId" to userId,
                "score" to score,
                "wordsCount" to wordsCount,
                "timestamp" to System.currentTimeMillis()
            )

            // ✅ Пытаемся сохранить, но не ждем бесконечно
            val saveJob = kotlinx.coroutines.GlobalScope.async {
                try {
                    firestore.collection("game_results")
                        .add(gameResult)
                        .await()

                    ServiceLocator.userRepository.updateGameScore(score)
                    Log.d(TAG, "Game result saved: score=$score")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save game result to Firestore", e)
                    // ✅ Сохраняем результат локально, если нет интернета
                    saveGameResultLocally(userId, score, wordsCount)
                }
            }

            // ✅ Ждем максимум 2 секунды на сохранение
            try {
                withTimeoutOrNull(2000) {
                    saveJob.await()
                }
                Log.d(TAG, "Game result saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Timeout saving game result", e)
                // Не блокируем игру, даже если сохранение не удалось
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error saving game result", e)
            // ✅ Даже при ошибке не выбрасываем исключение
        }
    }

    // ✅ Сохраняем результат локально, если нет интернета
    private fun saveGameResultLocally(userId: String, score: Int, wordsCount: Int) {
        try {
            val prefs = ServiceLocator.appContext.getSharedPreferences("pending_game_results", android.content.Context.MODE_PRIVATE)
            val pendingResults = prefs.getStringSet("pending_results", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

            val resultJson = "$userId|$score|$wordsCount|${System.currentTimeMillis()}"
            pendingResults.add(resultJson)

            prefs.edit().putStringSet("pending_results", pendingResults).apply()
            Log.d(TAG, "Game result saved locally, will sync later")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving game result locally", e)
        }
    }
}