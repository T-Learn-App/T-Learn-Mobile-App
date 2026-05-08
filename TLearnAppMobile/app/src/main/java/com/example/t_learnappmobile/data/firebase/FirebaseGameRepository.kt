package com.example.t_learnappmobile.data.firebase

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.domain.model.GameWord
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        val userId = auth.getUserId() ?: return
        try {
            val gameResult = mapOf(
                "userId" to userId,
                "score" to score,
                "wordsCount" to wordsCount,
                "timestamp" to System.currentTimeMillis() // Это Long
            )
            firestore.collection("game_results")
                .add(gameResult)
                .await()

            ServiceLocator.userRepository.updateGameScore(score)
            Log.d(TAG, "Game result saved: score=$score, timestamp=${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving game result", e)
        }
    }
}