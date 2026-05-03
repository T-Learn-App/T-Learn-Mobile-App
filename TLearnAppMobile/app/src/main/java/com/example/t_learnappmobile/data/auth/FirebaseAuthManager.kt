package com.example.t_learnappmobile.data.auth

import android.util.Log
import com.example.t_learnappmobile.data.repository.ServiceLocator
import com.example.t_learnappmobile.presentation.auth.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {
    private val auth: FirebaseAuth = Firebase.auth
    private val TAG = "FirebaseAuth"

    suspend fun login(email: String, password: String): AuthState {
        return try {
            Log.d(TAG, "Login: $email")
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            Log.d(TAG, "Login success: ${user?.uid}")

            AuthState.Success(
                userId = user?.uid?.toLongOrNull(),
                email = user?.email
            )
        } catch (e: Exception) {
            Log.e(TAG, "Login error", e)
            AuthState.Error(mapFirebaseAuthError(e))
        }
    }

    suspend fun register(email: String, password: String, firstName: String = "", lastName: String = ""): AuthState {
        return try {
            Log.d(TAG, "Register: $email, firstName: $firstName, lastName: $lastName")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            Log.d(TAG, "Register success: ${user?.uid}")

            user?.uid?.let { uid ->
                val userProfile = hashMapOf(
                    "email" to email,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "createdAt" to System.currentTimeMillis(),
                    "totalScore" to 0
                )
                ServiceLocator.firestore.collection("users")
                    .document(uid)
                    .set(userProfile)
                    .await()
                Log.d(TAG, "User profile created in Firestore: $userProfile")
            }

            AuthState.Success(
                userId = user?.uid?.toLongOrNull(),
                email = user?.email
            )
        } catch (e: Exception) {
            Log.e(TAG, "Register error", e)
            AuthState.Error(mapFirebaseAuthError(e))
        }
    }

    suspend fun logout() {
        Log.d(TAG, "Logout")
        auth.signOut()
    }

    fun getCurrentUser() = auth.currentUser
    fun getUserId(): String? = auth.currentUser?.uid
    fun getUserEmail(): String? = auth.currentUser?.email

    suspend fun getAccessToken(): String? {
        return try {
            auth.currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting token", e)
            null
        }
    }

    private fun mapFirebaseAuthError(e: Exception): String {
        return when {
            e.message?.contains("The email address is badly formatted") == true ->
                "Неверный формат почты"
            e.message?.contains("There is no user record") == true ->
                "Пользователь не найден"
            e.message?.contains("The password is invalid") == true ->
                "Неверный пароль"
            e.message?.contains("The email address is already in use") == true ->
                "Пользователь уже существует"
            e.message?.contains("Password should be at least 6 characters") == true ->
                "Пароль должен быть минимум 6 символов"
            else -> e.message ?: "Ошибка аутентификации"
        }
    }
}