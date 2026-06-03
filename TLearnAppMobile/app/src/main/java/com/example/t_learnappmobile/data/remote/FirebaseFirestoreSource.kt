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
            Log.d(TAG, "=== getWords START ===")
            Log.d(TAG, "Requested dictionaryId: '$dictionaryId'")
            Log.d(TAG, "dictionaryId length: ${dictionaryId.length}")
            Log.d(TAG, "dictionaryId chars: ${dictionaryId.toCharArray().joinToString(",")}")


            val querySnapshot = firestore.collection("words")
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()

            Log.d(TAG, "Query returned ${querySnapshot.documents.size} documents")

            querySnapshot.documents.forEach { doc ->
                val data = doc.data
                Log.d(TAG, "Document ${doc.id}: dictionaryId field = ${data?.get("dictionaryId")}")
                Log.d(TAG, "  - englishWord: ${data?.get("englishWord")}")
                Log.d(TAG, "  - translation: ${data?.get("translation")}")
            }

            val words = querySnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: run {
                    Log.w(TAG, "Document ${doc.id} has no data")
                    return@mapNotNull null
                }

                val docDictionaryId = data["dictionaryId"] as? String
                if (docDictionaryId != dictionaryId) {
                    Log.w(TAG, "Document ${doc.id} has dictionaryId='$docDictionaryId', expected '$dictionaryId'")
                }

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

            Log.d(TAG, "=== getWords END - returning ${words.size} words ===")
            words
        } catch (e: Exception) {
            Log.e(TAG, "Error getting words for dictionary '$dictionaryId'", e)
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


    suspend fun updateScore(uid: String, score: Int) {
        try {
            firestore.collection("users").document(uid)
                .update("totalScore", FieldValue.increment(score.toLong()))
                .await()

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