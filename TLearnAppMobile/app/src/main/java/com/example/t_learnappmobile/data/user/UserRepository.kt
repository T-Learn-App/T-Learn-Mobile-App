package com.example.t_learnappmobile.data.user

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String,
    val email: String?,
    val firstName: String = "",
    val lastName: String = "",
    val totalScore: Int = 0,
    val gamesPlayed: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

class UserRepository {
    private val TAG = "UserRepository"
    private val firestore = ServiceLocator.firestore
    private val authManager = ServiceLocator.firebaseAuthManager

    suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = authManager.getUserId() ?: return null

        return try {
            val document = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            if (document.exists()) {
                UserProfile(
                    uid = uid,
                    email = document.getString("email"),
                    firstName = document.getString("firstName") ?: "",
                    lastName = document.getString("lastName") ?: "",
                    totalScore = document.getLong("totalScore")?.toInt() ?: 0,
                    gamesPlayed = document.getLong("gamesPlayed")?.toInt() ?: 0,
                    createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
                )
            } else {
                UserProfile(uid = uid, email = authManager.getUserEmail())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile", e)
            null
        }
    }

    suspend fun updateProfile(firstName: String, lastName: String): Boolean {
        val uid = authManager.getUserId() ?: return false

        return try {
            firestore.collection("users")
                .document(uid)
                .set(
                    mapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            false
        }
    }

    suspend fun updateGameScore(score: Int) {
        val uid = authManager.getUserId() ?: return

        try {
            firestore.collection("users")
                .document(uid)
                .update(
                    mapOf(
                        "totalScore" to com.google.firebase.firestore.FieldValue.increment(score.toLong()),
                        "gamesPlayed" to com.google.firebase.firestore.FieldValue.increment(1)
                    )
                )
                .await()

            // Обновляем лидерборд
            updateLeaderboard(uid, score)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating score", e)
        }
    }

    private suspend fun updateLeaderboard(uid: String, score: Int) {
        try {
            val userProfile = getCurrentUserProfile()
            val userName = "${userProfile?.firstName ?: ""} ${userProfile?.lastName ?: ""}".trim()
            val displayName = if (userName.isNotEmpty()) userName else authManager.getUserEmail()?.split("@")?.first() ?: "User"

            firestore.collection("leaderboard")
                .document(uid)
                .set(
                    mapOf(
                        "userId" to uid,
                        "username" to displayName,
                        "totalScore" to com.google.firebase.firestore.FieldValue.increment(score.toLong()),
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating leaderboard", e)
        }
    }
}