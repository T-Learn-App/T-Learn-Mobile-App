package com.example.t_learnappmobile.data.remote

import android.util.Log
import com.example.t_learnappmobile.data.local.entities.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseFirestoreSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val TAG = "FirestoreSource"

    suspend fun getUserProgress(userId: String, dictionaryId: String): List<UserWordEntity> {
        return try {
            withTimeoutOrNull(5000L) {
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
            } ?: emptyList()
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
            val existingUser = firestore.collection("users").document(uid).get().await()
            val displayName = buildDisplayName(firstName, lastName, email)
            val currentTime = System.currentTimeMillis()

            if (!existingUser.exists()) {
                val userData = mapOf(
                    "email" to (email ?: ""),
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "totalScore" to 0,
                    "createdAt" to currentTime
                )

                firestore.collection("users").document(uid).set(userData).await()

                firestore.collection("leaderboard").document(uid).set(
                    mapOf(
                        "userId" to uid,
                        "username" to displayName,
                        "totalScore" to 0,
                        "updatedAt" to currentTime
                    )
                ).await()

                Log.d(TAG, "Created new user profile for: $uid")
            } else {
                Log.d(TAG, "User already exists: $uid")
                val currentData = existingUser.data ?: return
                val currentFirstName = currentData["firstName"] as? String ?: ""
                val currentLastName = currentData["lastName"] as? String ?: ""

                if (currentFirstName.isEmpty() && currentLastName.isEmpty()) {
                    firestore.collection("users").document(uid).update(
                        mapOf(
                            "firstName" to firstName,
                            "lastName" to lastName
                        )
                    ).await()

                    firestore.collection("leaderboard").document(uid).update(
                        mapOf(
                            "username" to displayName
                        )
                    ).await()
                    Log.d(TAG, "Updated user name for: $uid")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/updating profile", e)
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
            withTimeoutOrNull(5000L) {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()

                val displayName = buildDisplayName(firstName, lastName, null)
                firestore.collection("leaderboard").document(uid).update(
                    mapOf(
                        "username" to displayName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Log.d(TAG, "Profile updated for user: $uid")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
        }
    }

    suspend fun getWords(dictionaryId: String): List<WordEntity> {
        return try {
            withTimeoutOrNull(5000L) {
                val querySnapshot = firestore.collection("words")
                    .whereEqualTo("dictionaryId", dictionaryId)
                    .get()
                    .await()

                querySnapshot.documents.mapNotNull { doc ->
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
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting words", e)
            emptyList()
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

    suspend fun updateScore(uid: String, score: Int) {
        try {
            val userRef = firestore.collection("users").document(uid)
            val userDoc = userRef.get().await()

            if (userDoc.exists()) {
                userRef.update("totalScore", FieldValue.increment(score.toLong())).await()
                Log.d(TAG, "Updated user totalScore: +$score for $uid")
            } else {
                userRef.set(
                    mapOf(
                        "totalScore" to score,
                        "createdAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).await()
                Log.d(TAG, "Created user document with score: $score for $uid")
            }

            val leaderboardRef = firestore.collection("leaderboard").document(uid)
            val leaderboardDoc = leaderboardRef.get().await()

            if (leaderboardDoc.exists()) {
                leaderboardRef.update(
                    mapOf(
                        "totalScore" to FieldValue.increment(score.toLong()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Log.d(TAG, "Updated leaderboard totalScore: +$score for $uid")
            } else {
                val userData = userDoc.data ?: mapOf()
                val firstName = userData["firstName"] as? String ?: ""
                val lastName = userData["lastName"] as? String ?: ""
                val username = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                    "$firstName $lastName".trim()
                } else {
                    userData["email"] as? String ?: uid
                }

                leaderboardRef.set(
                    mapOf(
                        "userId" to uid,
                        "username" to username,
                        "totalScore" to score,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Log.d(TAG, "Created leaderboard entry with score: $score for $uid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating score", e)
            throw e
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
    suspend fun deleteUserProgress(userId: String, dictionaryId: String) {
        try {
            val snapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .whereEqualTo("dictionaryId", dictionaryId)
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Log.d(TAG, "Deleted user progress for userId=$userId, dictionaryId=$dictionaryId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user progress", e)
        }
    }

    suspend fun deleteAllUserProgress(userId: String) {
        try {
            val snapshot = firestore.collection("user_words")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Log.d(TAG, "Deleted all user progress for userId=$userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all user progress", e)
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
}