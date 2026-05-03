package com.example.t_learnappmobile.data.user

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String,
    val email: String?,
    val firstName: String = "",
    val lastName: String = "",
    val totalScore: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

class UserRepository {
    private val TAG = "UserRepository"
    private val firestore = ServiceLocator.firestore
    private val authManager = ServiceLocator.firebaseAuthManager

    suspend fun createUserProfile(
        uid: String,
        email: String?,
        firstName: String = "",
        lastName: String = ""
    ): Boolean {
        return try {
            val userProfile = mapOf(
                "email" to (email ?: ""),
                "firstName" to firstName,
                "lastName" to lastName,
                "totalScore" to 0,
                "createdAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(uid)
                .set(userProfile)
                .await()

            Log.d(TAG, "User profile created: $userProfile")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user profile", e)
            false
        }
    }

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
            // Обновляем счет пользователя
            firestore.collection("users")
                .document(uid)
                .update("totalScore", FieldValue.increment(score.toLong()))
                .await()

            // Получаем профиль для имени
            val userProfile = getCurrentUserProfile()
            val firstName = userProfile?.firstName ?: ""
            val lastName = userProfile?.lastName ?: ""
            val displayName = if (firstName.isNotEmpty()) {
                "${firstName} ${lastName.firstOrNull() ?: ""}."
            } else {
                authManager.getUserEmail()?.split("@")?.firstOrNull() ?: "User"
            }

            // Обновляем таблицу лидеров
            firestore.collection("leaderboard")
                .document(uid)
                .set(
                    mapOf(
                        "userId" to uid,
                        "username" to displayName,
                        "totalScore" to FieldValue.increment(score.toLong()),
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()

            Log.d(TAG, "Game score updated: +$score for user $displayName")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating score", e)
        }
    }
}