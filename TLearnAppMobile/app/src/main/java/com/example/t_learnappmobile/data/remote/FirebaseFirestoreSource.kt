// data/remote/FirebaseFirestoreSource.kt
package com.example.t_learnappmobile.data.remote

import android.util.Log
import com.example.t_learnappmobile.data.local.entities.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseFirestoreSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val TAG = "FirestoreSource"

    suspend fun getWords(dictionaryId: String): List<WordEntity> {
        return try {
            firestore.collection("words")
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    WordEntity(
                        id = doc.id,
                        dictionaryId = dictionaryId,
                        englishWord = data["englishWord"] as? String ?: "",
                        translation = data["translation"] as? String ?: "",
                        transcription = data["transcription"] as? String ?: "",
                        partOfSpeech = data["partOfSpeech"] as? String ?: "",
                        lastUpdated = System.currentTimeMillis()
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting words", e)
            emptyList()
        }
    }

    suspend fun getUserProgress(userId: String, dictionaryId: String): List<UserWordEntity> {
        return try {
            firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    UserWordEntity(
                        userId = userId,
                        wordId = data["wordId"] as? String ?: return@mapNotNull null,
                        dictionaryId = dictionaryId,
                        stage = (data["stage"] as? Long)?.toInt() ?: 0,
                        nextReviewDate = data["nextReviewDate"] as? Long ?: 0,
                        failCount = (data["failCount"] as? Long)?.toInt() ?: 0,
                        lastReviewDate = data["lastReviewDate"] as? Long,
                        totalViews = (data["totalViews"] as? Long)?.toInt() ?: 0,
                        correctCount = (data["correctCount"] as? Long)?.toInt() ?: 0,
                        incorrectCount = (data["incorrectCount"] as? Long)?.toInt() ?: 0,
                        isSynced = true,
                        updatedAt = System.currentTimeMillis()
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting progress", e)
            emptyList()
        }
    }

    suspend fun saveUserProgress(userWord: UserWordEntity) {
        try {
            val docId = "${userWord.userId}_${userWord.wordId}"
            val data = mapOf(
                "userId" to userWord.userId,
                "wordId" to userWord.wordId,
                "dictionaryId" to userWord.dictionaryId,
                "stage" to userWord.stage,
                "nextReviewDate" to userWord.nextReviewDate,
                "failCount" to userWord.failCount,
                "lastReviewDate" to userWord.lastReviewDate,
                "totalViews" to userWord.totalViews,
                "correctCount" to userWord.correctCount,
                "incorrectCount" to userWord.incorrectCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("user_words")
                .document(docId)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving progress", e)
        }
    }

    suspend fun createUserProfile(
        uid: String,
        email: String?,
        firstName: String,
        lastName: String
    ) {
        try {
            val userData = mapOf(
                "email" to (email ?: ""),
                "firstName" to firstName,
                "lastName" to lastName,
                "totalScore" to 0,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("users").document(uid).set(userData).await()

            val displayName = buildDisplayName(firstName, lastName, email)
            firestore.collection("leaderboard").document(uid).set(
                mapOf(
                    "userId" to uid,
                    "username" to displayName,
                    "totalScore" to 0,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating profile", e)
        }
    }

    suspend fun getUserProfile(uid: String): Map<String, Any?>? {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile", e)
            null
        }
    }

    suspend fun updateProfile(uid: String, firstName: String, lastName: String) {
        try {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
        }
    }

// data/remote/FirebaseFirestoreSource.kt
// Найдите метод updateScore и убедитесь, что он делает только ОДНО обновление

    suspend fun updateScore(uid: String, score: Int) {
        try {
            // Обновляем счет в профиле пользователя
            firestore.collection("users").document(uid)
                .update("totalScore", FieldValue.increment(score.toLong()))
                .await()

            // Обновляем счет в таблице лидеров
            firestore.collection("leaderboard").document(uid)
                .update(
                    mapOf(
                        "totalScore" to FieldValue.increment(score.toLong()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Log.d(TAG, "Score updated: +$score for user $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating score", e)
            // Если документ в leaderboard не существует, создаем его
            try {
                firestore.collection("leaderboard").document(uid)
                    .set(
                        mapOf(
                            "userId" to uid,
                            "totalScore" to score,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                    .await()
            } catch (e2: Exception) {
                Log.e(TAG, "Error creating leaderboard entry", e2)
            }
        }
    }
    // data/remote/FirebaseFirestoreSource.kt
// Добавьте этот метод в класс FirebaseFirestoreSource

    suspend fun resetUserWordProgress(userId: String, wordId: String, dictionaryId: String) {
        try {
            val docId = "${userId}_${wordId}"
            val resetData = mapOf(
                "userId" to userId,
                "wordId" to wordId,
                "dictionaryId" to dictionaryId,
                "stage" to 0,
                "nextReviewDate" to System.currentTimeMillis(),
                "failCount" to 0,
                "lastReviewDate" to null,
                "totalViews" to 0,
                "correctCount" to 0,
                "incorrectCount" to 0,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("user_words")
                .document(docId)
                .set(resetData, SetOptions.merge())
                .await()

            Log.d(TAG, "Reset progress for word: $wordId")
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting word progress", e)
        }
    }

    suspend fun getDictionaries(): List<DictionaryEntity> {
        return try {
            firestore.collection("dictionaries")
                .orderBy("order")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    DictionaryEntity(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        order = (data["order"] as? Long)?.toInt() ?: 0
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting dictionaries", e)
            emptyList()
        }
    }

    suspend fun resetUserWords(userId: String, dictionaryId: String) {
        try {
            val snapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting words", e)
        }
    }

    suspend fun deleteGameResults(userId: String) {
        try {
            val snapshot = firestore.collection("game_results")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting game results", e)
        }
    }

    private fun buildDisplayName(firstName: String, lastName: String, email: String?): String {
        return when {
            firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName ${lastName.first().uppercase()}."
            firstName.isNotEmpty() -> firstName
            lastName.isNotEmpty() -> lastName
            !email.isNullOrEmpty() -> email.split("@").firstOrNull() ?: "User"
            else -> "User"
        }
    }
}