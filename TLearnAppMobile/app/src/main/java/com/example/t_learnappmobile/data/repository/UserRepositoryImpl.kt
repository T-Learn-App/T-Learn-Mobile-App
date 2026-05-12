// data/repository/UserRepositoryImpl.kt
package com.example.t_learnappmobile.data.repository

import android.util.Log
import com.example.t_learnappmobile.data.remote.FirebaseAuthSource
import com.example.t_learnappmobile.data.remote.FirebaseFirestoreSource
import com.example.t_learnappmobile.domain.model.UserProfile
import com.example.t_learnappmobile.domain.repository.UserRepository

class UserRepositoryImpl(
    private val firestoreSource: FirebaseFirestoreSource,
    private val authSource: FirebaseAuthSource
) : UserRepository {

    private val TAG = "UserRepository"

    override suspend fun getUserProfile(userId: String): UserProfile? {
        val data = firestoreSource.getUserProfile(userId) ?: return null
        return UserProfile(
            uid = userId,
            email = data["email"] as? String,
            firstName = data["firstName"] as? String ?: "",
            lastName = data["lastName"] as? String ?: "",
            totalScore = (data["totalScore"] as? Long)?.toInt() ?: 0,
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
        )
    }

    override suspend fun createUserProfile(
        uid: String,
        email: String?,
        firstName: String,
        lastName: String
    ): Result<Unit> {
        return try {
            firestoreSource.createUserProfile(uid, email, firstName, lastName)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating profile", e)
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(firstName: String, lastName: String): Result<Unit> {
        val uid = authSource.getCurrentUserId() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            firestoreSource.updateProfile(uid, firstName, lastName)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile", e)
            Result.failure(e)
        }
    }

    override suspend fun updateScore(score: Int): Result<Unit> {
        val uid = authSource.getCurrentUserId() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            firestoreSource.updateScore(uid, score)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating score", e)
            Result.failure(e)
        }
    }

    override suspend fun resetUserData(userId: String): Result<Unit> {
        return try {
            firestoreSource.deleteGameResults(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting user data", e)
            Result.failure(e)
        }
    }
}