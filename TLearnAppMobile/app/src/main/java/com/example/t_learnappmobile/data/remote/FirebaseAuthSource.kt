// data/remote/FirebaseAuthSource.kt
package com.example.t_learnappmobile.data.remote

import android.util.Log
import com.example.t_learnappmobile.domain.model.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirebaseAuthSource {
    private val auth: FirebaseAuth = Firebase.auth
    private val TAG = "FirebaseAuth"

    suspend fun signIn(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("User is null"))
            Result.success(AuthResult(user.uid, user.email))
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error", e)
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    suspend fun signUp(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("User is null"))
            Result.success(AuthResult(user.uid, user.email))
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error", e)
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth.currentUser?.email
    fun isAuthenticated(): Boolean = auth.currentUser != null

    private fun mapFirebaseError(e: Exception): String {
        val message = e.message ?: return "Authentication error"
        return when {
            message.contains("The email address is badly formatted") -> "Invalid email format"
            message.contains("There is no user record") -> "User not found"
            message.contains("The password is invalid") -> "Invalid password"
            message.contains("The email address is already in use") -> "Email already in use"
            message.contains("Password should be at least 6 characters") -> "Password too short"
            message.contains("A network error") -> "Network error"
            else -> message
        }
    }
}